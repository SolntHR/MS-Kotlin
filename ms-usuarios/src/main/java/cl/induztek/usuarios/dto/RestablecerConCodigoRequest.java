package cl.induztek.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RestablecerConCodigoRequest(
        @NotBlank(message = "Ingresa tu correo electrónico")
        @Email(message = "El correo no tiene un formato válido")
        String correo,

        @NotBlank(message = "Ingresa el código que recibiste")
        @Pattern(regexp = "^\\d{6}$", message = "El código tiene 6 dígitos")
        String codigo,

        @NotBlank(message = "Escribe la nueva contraseña")
        @Pattern(regexp = "^[0-9a-f]{64}$", message = "La contraseña debe llegar cifrada con SHA-256 desde la app")
        String nuevaContrasena) {
}
