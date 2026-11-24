package cl.induztek.usuarios.dto;

import cl.induztek.usuarios.model.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CambiarRolRequest(
        @NotNull(message = "Elige el perfil del usuario")
        Rol rol,
        @NotNull(message = "Indica quién hace el cambio")
        Long jefeId,
        @NotBlank(message = "Ingresa tu contraseña para confirmar")
        String contrasena) {
}
