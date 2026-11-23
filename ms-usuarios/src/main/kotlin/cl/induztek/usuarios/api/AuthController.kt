package cl.induztek.usuarios.api

import cl.induztek.usuarios.service.AuthService
import cl.induztek.usuarios.service.UsuarioService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación")
class AuthController(private val usuarios: UsuarioService, private val auth: AuthService) {
    @PostMapping("/registro")
    @Operation(summary = "Registrar cuenta pendiente, sin contraseña ni rol", responses = [ApiResponse(responseCode = "201", description = "Cuenta creada"), ApiResponse(responseCode = "400", description = "Validación"), ApiResponse(responseCode = "409", description = "Duplicado"), ApiResponse(responseCode = "429", description = "Límite de intentos")])
    fun registro(@Valid @RequestBody r: RegistroRequest): ResponseEntity<RegistroResponse> {
        val creado = usuarios.registrar(r)
        return ResponseEntity.created(URI("/api/v1/usuarios/${creado.id}")).body(creado)
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión con correo interno y contraseña", responses = [ApiResponse(responseCode = "200", description = "Token Bearer"), ApiResponse(responseCode = "401", description = "Credenciales inválidas"), ApiResponse(responseCode = "403", description = "Cuenta desactivada"), ApiResponse(responseCode = "429", description = "Límite de intentos")])
    fun login(@Valid @RequestBody r: LoginRequest) = auth.login(r)

    @PostMapping("/recuperacion/solicitar")
    @Operation(summary = "Enviar código al correo personal si coinciden los datos", responses = [ApiResponse(responseCode = "202", description = "Solicitud procesada sin revelar existencia de la cuenta"), ApiResponse(responseCode = "400", description = "Validación"), ApiResponse(responseCode = "429", description = "Límite de intentos"), ApiResponse(responseCode = "503", description = "Correo no disponible")])
    fun solicitar(@Valid @RequestBody r: RecuperacionRequest): ResponseEntity<MensajeResponse> {
        auth.solicitarCodigo(r)
        return ResponseEntity.accepted().body(MensajeResponse("Si los datos coinciden con una cuenta habilitada, recibirás un código en tu correo personal"))
    }

    @PostMapping("/recuperacion/restablecer")
    @Operation(summary = "Establecer contraseña con código de un solo uso y revocar sesiones", responses = [ApiResponse(responseCode = "204", description = "Contraseña guardada"), ApiResponse(responseCode = "400", description = "Validación o código inválido"), ApiResponse(responseCode = "429", description = "Límite de intentos")])
    fun restablecer(@Valid @RequestBody r: RestablecerRequest): ResponseEntity<Void> {
        auth.restablecer(r)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Revocar la sesión actual", responses = [ApiResponse(responseCode = "204", description = "Sesión revocada"), ApiResponse(responseCode = "401", description = "Token inválido")])
    fun logout(@RequestHeader("Authorization") authorization: String): ResponseEntity<Void> {
        auth.logout(authorization.substring(7))
        return ResponseEntity.noContent().build()
    }
}
