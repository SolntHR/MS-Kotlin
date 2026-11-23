package cl.induztek.usuarios.api

import cl.induztek.usuarios.domain.Rol
import cl.induztek.usuarios.domain.Usuario
import jakarta.validation.constraints.*
import java.time.Instant

// Deliberadamente no son data classes: toString() nunca debe imprimir secretos.
class RegistroRequest(
    @field:NotBlank @field:Size(max = 150) val nombre: String,
    @field:NotBlank @field:Size(max = 15) val rut: String,
    @field:NotBlank @field:Size(max = 25) val telefono: String,
    @field:NotBlank @field:Email @field:Size(max = 254) val correoPersonal: String,
    @field:NotBlank @field:Size(max = 150) val preguntaSeguridad: String,
    @field:NotBlank @field:Size(max = 128) val respuestaSeguridad: String
)

class LoginRequest(
    @field:NotBlank @field:Email @field:Size(max = 254) val correo: String,
    @field:NotBlank @field:Size(max = 128) val contrasena: String
)

class RecuperacionRequest(
    @field:NotBlank @field:Email @field:Size(max = 254) val correo: String,
    @field:NotBlank @field:Size(max = 150) val preguntaSeguridad: String,
    @field:NotBlank @field:Size(max = 128) val respuestaSeguridad: String
)

class RestablecerRequest(
    @field:NotBlank @field:Email @field:Size(max = 254) val correo: String,
    @field:Pattern(regexp = "[0-9]{8}") val codigo: String,
    @field:NotBlank @field:Size(min = 12, max = 128) val nuevaContrasena: String,
    @field:NotBlank @field:Size(max = 128) val confirmacion: String
)

class RolRequest(val rolCodigo: Rol)
class ActivoRequest(val activo: Boolean)

data class RegistroResponse(val id: Long, val correoInterno: String, val estado: String = "PENDIENTE_ASIGNACION_ROL")
data class MensajeResponse(val mensaje: String)
data class LoginResponse(val accessToken: String, val tokenType: String = "Bearer", val expiresIn: Long, val usuario: UsuarioResponse)
data class UsuarioResponse(
    val id: Long, val nombre: String, val rut: String, val telefono: String,
    val correoPersonal: String, val correoInterno: String, val rolCodigo: Rol?,
    val activo: Boolean, val credencialesConfiguradas: Boolean, val creadoEn: Instant
)

fun Usuario.publico() = UsuarioResponse(id, nombre, rut, telefono, correoPersonal, correoInterno, rolCodigo, activo, passwordHash != null, creadoEn)

data class ErrorResponse(
    val codigo: String,
    val mensaje: String,
    val errores: Map<String, String> = emptyMap(),
    val timestamp: Instant = Instant.now()
)
