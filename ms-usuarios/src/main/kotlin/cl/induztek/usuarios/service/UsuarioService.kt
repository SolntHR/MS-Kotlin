package cl.induztek.usuarios.service

import cl.induztek.usuarios.api.*
import cl.induztek.usuarios.domain.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration

@Service
class UsuarioService(
    private val usuarios: UsuarioRepository,
    private val sesiones: SesionRepository,
    private val codigos: CodigoRepository,
    private val validator: RegistroValidator,
    private val encoder: PasswordEncoder,
    private val clock: Clock,
    private val limiter: RateLimiter,
    @Value("\${app.internal-domain}") private val dominio: String
) {
    init { require(dominio.matches(Regex("[a-z0-9.-]+\\.[a-z]{2,}")) && dominio.length <= 190) { "INTERNAL_EMAIL_DOMAIN inválido" } }

    @Transactional
    fun registrar(r: RegistroRequest): RegistroResponse {
        limiter.verificar("registro", validator.correo(r.correoPersonal), 5, Duration.ofMinutes(15))
        validator.validar(r)
        val rut = validator.rut(r.rut)
        val correo = validator.correo(r.correoPersonal)
        if (usuarios.existsByRut(rut) || usuarios.existsByCorreoPersonal(correo))
            throw ApiException(HttpStatus.CONFLICT, "USUARIO_DUPLICADO", "El RUT o correo ya está registrado")
        val usuario = usuarios.saveAndFlush(Usuario(
            nombre = validator.nombre(r.nombre), rut = rut, telefono = validator.telefono(r.telefono),
            correoPersonal = correo, correoInterno = validator.correoInterno(r.nombre, rut, dominio),
            preguntaSeguridad = r.preguntaSeguridad, respuestaHash = encoder.encode(validator.respuesta(r.respuestaSeguridad)),
            creadoEn = clock.instant()
        ))
        return RegistroResponse(usuario.id, usuario.correoInterno)
    }

    @Transactional(readOnly = true)
    fun obtener(id: Long): UsuarioResponse = buscar(id).publico()

    @Transactional(readOnly = true)
    fun listar(pagina: Int, tamano: Int): Page<UsuarioResponse> {
        if (pagina < 0 || tamano !in 1..100) throw ApiException(HttpStatus.BAD_REQUEST, "VALIDACION", "pagina debe ser >= 0 y tamano entre 1 y 100")
        return usuarios.findAll(PageRequest.of(pagina, tamano, Sort.by("id"))).map { it.publico() }
    }

    @Transactional
    fun asignarRol(actorId: Long, id: Long, rol: Rol): UsuarioResponse {
        val admins = verificarAdministrador(actorId)
        val usuario = usuarios.bloquear(id) ?: noEncontrado()
        if (usuario.rolCodigo == Rol.ADMIN && rol != Rol.ADMIN && usuario.activo && admins.count { it.activo } <= 1)
            throw ApiException(HttpStatus.CONFLICT, "ULTIMO_ADMINISTRADOR", "Debe quedar al menos un administrador activo")
        usuario.rolCodigo = rol
        usuario.activo = true
        sesiones.revocarUsuario(id)
        codigos.deleteById(id)
        return usuario.publico()
    }

    @Transactional
    fun cambiarActivo(actorId: Long, id: Long, activo: Boolean): UsuarioResponse {
        val admins = verificarAdministrador(actorId)
        val usuario = usuarios.bloquear(id) ?: noEncontrado()
        if (activo && usuario.rolCodigo == null) throw ApiException(HttpStatus.CONFLICT, "ROL_REQUERIDO", "Asigna un rol antes de activar la cuenta")
        if (!activo && usuario.activo && usuario.rolCodigo == Rol.ADMIN && admins.count { it.activo } <= 1)
            throw ApiException(HttpStatus.CONFLICT, "ULTIMO_ADMINISTRADOR", "Debe quedar al menos un administrador activo")
        usuario.activo = activo
        sesiones.revocarUsuario(id)
        codigos.deleteById(id)
        return usuario.publico()
    }

    private fun verificarAdministrador(id: Long): List<Usuario> {
        // Serializa cambios de administradores y vuelve a comprobar permisos tras adquirir el bloqueo.
        val admins = usuarios.bloquearAdministradores()
        if (admins.none { it.id == id && it.activo }) throw ApiException(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", "Se requiere un administrador activo")
        return admins
    }
    private fun buscar(id: Long) = usuarios.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "USUARIO_NO_ENCONTRADO", "Usuario no encontrado") }
    private fun noEncontrado(): Nothing = throw ApiException(HttpStatus.NOT_FOUND, "USUARIO_NO_ENCONTRADO", "Usuario no encontrado")
}
