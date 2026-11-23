package cl.induztek.usuarios.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarActivoRequest(
        @NotNull(message = "Indica si la cuenta queda activa")
        Boolean activo) {
}
