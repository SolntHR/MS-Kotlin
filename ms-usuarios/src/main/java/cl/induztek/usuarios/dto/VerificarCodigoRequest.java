package cl.induztek.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificarCodigoRequest(
        @NotBlank(message = "Ingresa tu correo electrónico")
        @Email(message = "El correo no tiene un formato válido")
        String correo,

        @NotBlank(message = "Ingresa el código que recibiste")
        @Pattern(regexp = "^\\d{6}$", message = "El código tiene 6 dígitos")
        String codigo) {
}
