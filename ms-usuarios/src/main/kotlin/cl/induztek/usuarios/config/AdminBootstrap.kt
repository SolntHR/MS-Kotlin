package cl.induztek.usuarios.config

import cl.induztek.usuarios.domain.Rol
import cl.induztek.usuarios.domain.Usuario
import cl.induztek.usuarios.domain.UsuarioRepository
import cl.induztek.usuarios.service.RegistroValidator
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Component
class AdminBootstrap(
    private val usuarios: UsuarioRepository,
    private val encoder: PasswordEncoder,
    private val validator: RegistroValidator,
    private val clock: Clock,
    @Value("\${app.bootstrap.email}") private val email: String,
    @Value("\${app.bootstrap.password}") private val password: String,
    @Value("\${app.bootstrap.answer}") private val answer: String
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        if (email.isBlank()) {
            require(password.isBlank() && answer.isBlank()) { "Configura también ADMIN_EMAIL" }
            return
        }
        val correo = validator.correo(email)
        require(correo.matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))) { "ADMIN_EMAIL inválido" }
        val existente = usuarios.findByCorreoInterno(correo)
        if (existente != null) {
            require(existente.rolCodigo == Rol.ADMIN) { "ADMIN_EMAIL ya pertenece a una cuenta sin rol ADMIN" }
            return // Nunca sobrescribir claves ni reactivar cuentas al reiniciar.
        }
        require(password.length in 12..128 && password.isNotBlank()) { "ADMIN_PASSWORD debe tener entre 12 y 128 caracteres" }
        require(answer.isNotBlank() && answer.length <= 128) { "Configura ADMIN_SECURITY_ANSWER" }
        require(usuarios.count() == 0L) { "Bootstrap solo permitido en una base vacía; usa un ADMIN existente" }
        usuarios.saveAndFlush(Usuario(
            nombre = "Administrador Induztek", rut = "000000000", telefono = "+56 9 0000 0000",
            correoPersonal = correo, correoInterno = correo, preguntaSeguridad = validator.preguntas.first(),
            respuestaHash = encoder.encode(validator.respuesta(answer)), passwordHash = encoder.encode(password),
            rolCodigo = Rol.ADMIN, activo = true, creadoEn = clock.instant()
        ))
    }
}
