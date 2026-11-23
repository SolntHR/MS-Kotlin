package cl.induztek.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CambiarContrasenaRequest(
        @NotBlank(message = "Escribe tu contraseña actual")
        String actual,

        @NotBlank(message = "Escribe la nueva contraseña")
        @Pattern(regexp = "^[0-9a-f]{64}$", message = "La contraseña debe llegar cifrada con SHA-256 desde la app")
        String nueva) {
}
