package cl.induztek.usuarios.api

import cl.induztek.usuarios.security.UsuarioPrincipal
import cl.induztek.usuarios.service.UsuarioService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

data class UsuariosPage(val contenido: List<UsuarioResponse>, val pagina: Int, val tamano: Int, val totalElementos: Long, val totalPaginas: Int)

@RestController
@RequestMapping("/api/v1/usuarios")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuarios", description = "Gestión reservada a ADMIN; /me admite cualquier sesión")
class UsuarioController(private val usuarios: UsuarioService) {
    @GetMapping("/me")
    @Operation(summary = "Consultar el perfil de la sesión actual")
    @ApiResponses(value = [ApiResponse(responseCode = "401", description = "Token inválido"), ApiResponse(responseCode = "404", description = "Usuario no encontrado")])
    fun me(@AuthenticationPrincipal principal: UsuarioPrincipal) = usuarios.obtener(principal.id)

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar usuarios, incluidos los pendientes (ADMIN)")
    @ApiResponses(value = [ApiResponse(responseCode = "400", description = "Paginación inválida"), ApiResponse(responseCode = "401", description = "Token inválido"), ApiResponse(responseCode = "403", description = "Requiere ADMIN")])
    fun listar(@RequestParam(defaultValue = "0") pagina: Int, @RequestParam(defaultValue = "20") tamano: Int): UsuariosPage {
        val page = usuarios.listar(pagina, tamano)
        return UsuariosPage(page.content, page.number, page.size, page.totalElements, page.totalPages)
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Consultar usuario por ID (ADMIN)")
    @ApiResponses(value = [ApiResponse(responseCode = "400", description = "ID inválido"), ApiResponse(responseCode = "401", description = "Token inválido"), ApiResponse(responseCode = "403", description = "Requiere ADMIN"), ApiResponse(responseCode = "404", description = "Usuario no encontrado")])
    fun obtener(@PathVariable id: Long) = usuarios.obtener(id)

    @PatchMapping("/{id}/rol")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Asignar rol y habilitar cuenta (ADMIN)")
    @ApiResponses(value = [ApiResponse(responseCode = "400", description = "Rol inválido"), ApiResponse(responseCode = "401", description = "Token inválido"), ApiResponse(responseCode = "403", description = "Requiere ADMIN"), ApiResponse(responseCode = "404", description = "Usuario no encontrado"), ApiResponse(responseCode = "409", description = "Último administrador")])
    fun rol(@AuthenticationPrincipal principal: UsuarioPrincipal, @PathVariable id: Long, @Valid @RequestBody r: RolRequest) = usuarios.asignarRol(principal.id, id, r.rolCodigo)

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activar o desactivar cuenta y revocar sesiones (ADMIN)")
    @ApiResponses(value = [ApiResponse(responseCode = "400", description = "JSON inválido"), ApiResponse(responseCode = "401", description = "Token inválido"), ApiResponse(responseCode = "403", description = "Requiere ADMIN"), ApiResponse(responseCode = "404", description = "Usuario no encontrado"), ApiResponse(responseCode = "409", description = "Falta rol o último administrador")])
    fun activo(@AuthenticationPrincipal principal: UsuarioPrincipal, @PathVariable id: Long, @Valid @RequestBody r: ActivoRequest) = usuarios.cambiarActivo(principal.id, id, r.activo)
}
