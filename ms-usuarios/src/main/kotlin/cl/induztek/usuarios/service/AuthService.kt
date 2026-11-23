package cl.induztek.usuarios.service

import cl.induztek.usuarios.api.*
import cl.induztek.usuarios.domain.*
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration

@Service
class AuthService(
    private val usuarios: UsuarioRepository,
    private val sesiones: SesionRepository,
    private val codigos: CodigoRepository,
    private val encoder: PasswordEncoder,
    private val secrets: SecretService,
    private val mailer: RecoveryMailer,
    private val validator: RegistroValidator,
    private val limiter: RateLimiter,
    private val clock: Clock
) {
    private val dummyHash = encoder.encode(secrets.token())

    @Transactional
    fun login(r: LoginRequest): LoginResponse {
        val correo = validator.correo(r.correo)
        limiter.verificar("login", correo, 10, Duration.ofMinutes(15))
        val usuario = usuarios.buscarIdPorCorreoInterno(correo)?.let { usuarios.bloquear(it) }
        // Mismo coste de hash cuando la cuenta no existe o no ha establecido contraseña.
        val coincide = encoder.matches(r.contrasena, usuario?.passwordHash ?: dummyHash)
        if (!coincide || usuario == null) credencialesInvalidas()
        // El bloqueo serializa login con restablecimiento, cambio de rol y desactivación.
        if (!usuario.activo || usuario.rolCodigo == null) throw ApiException(HttpStatus.FORBIDDEN, "CUENTA_NO_HABILITADA", "La cuenta no está habilitada. Contacta al administrador")
        val token = secrets.token()
        sesiones.eliminarExpiradas(clock.instant())
        sesiones.save(Sesion(secrets.tokenHash(token), usuario, clock.instant().plus(Duration.ofHours(12))))
        return LoginResponse(token, expiresIn = Duration.ofHours(12).seconds, usuario = usuario.publico())
    }

    @Transactional
    fun logout(token: String) { sesiones.deleteById(secrets.tokenHash(token)) }

    @Transactional
    fun solicitarCodigo(r: RecuperacionRequest) {
        val correo = validator.correo(r.correo)
        limiter.verificar("recuperacion", correo, 5, Duration.ofMinutes(15))
        val usuario = usuarios.buscarIdPorCorreoInterno(correo)?.let { usuarios.bloquear(it) }
        val coincide = encoder.matches(validator.respuesta(r.respuestaSeguridad), usuario?.respuestaHash ?: dummyHash)
        if (usuario == null || !coincide || usuario.preguntaSeguridad != r.preguntaSeguridad) return
        if (!usuario.activo || usuario.rolCodigo == null) return
        val ahora = clock.instant()
        val anterior = codigos.findById(usuario.id).orElse(null)
        // Respuesta idéntica durante el enfriamiento: no enviar códigos repetidos.
        if (anterior != null && anterior.enviadoEn.plusSeconds(60) > ahora) return
        var codigo = secrets.codigo()
        while (anterior != null && secrets.coincide(anterior.codigoHash, secrets.codigoHash(usuario.id, codigo))) codigo = secrets.codigo()
        codigos.save(CodigoRecuperacion(usuario.id, secrets.codigoHash(usuario.id, codigo), ahora.plusSeconds(600), 0, ahora))
        // Si falla SMTP, la transacción revierte el código nuevo; no se acepta un código sin envío.
        mailer.enviar(usuario.correoPersonal, codigo)
    }

    @Transactional(noRollbackFor = [ApiException::class])
    fun restablecer(r: RestablecerRequest) {
        if (r.nuevaContrasena != r.confirmacion) throw ApiException(HttpStatus.BAD_REQUEST, "VALIDACION", "Las contraseñas no coinciden", mapOf("confirmacion" to "Repite la misma contraseña"))
        val correo = validator.correo(r.correo)
        limiter.verificar("restablecer", correo, 10, Duration.ofMinutes(15))
        val id = usuarios.buscarIdPorCorreoInterno(correo) ?: codigoInvalido()
        val usuario = usuarios.bloquear(id) ?: codigoInvalido()
        val codigo = codigos.findById(usuario.id).orElse(null) ?: codigoInvalido()
        if (!usuario.activo || usuario.rolCodigo == null || codigo.expiraEn <= clock.instant() || codigo.intentos >= 5) codigoInvalido()
        if (!secrets.coincide(codigo.codigoHash, secrets.codigoHash(usuario.id, r.codigo))) {
            codigo.intentos++
            codigos.save(codigo)
            codigoInvalido()
        }
        usuario.passwordHash = encoder.encode(r.nuevaContrasena)
        codigos.delete(codigo)
        sesiones.revocarUsuario(usuario.id)
    }

    private fun codigoInvalido(): Nothing = throw ApiException(HttpStatus.BAD_REQUEST, "CODIGO_INVALIDO", "Código inválido, vencido o agotado. Solicita uno nuevo")
    private fun credencialesInvalidas(): Nothing = throw ApiException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos")
}
