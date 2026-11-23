package cl.induztek.usuarios.dto;

import cl.induztek.usuarios.model.Rol;
import jakarta.validation.constraints.NotNull;

public record CambiarRolRequest(
        @NotNull(message = "Elige el perfil del usuario")
        Rol rol) {
}
