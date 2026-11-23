package cl.induztek.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Ingresa tu correo electrónico")
        @Email(message = "El correo no tiene un formato válido")
        String correo,

        @NotBlank(message = "Ingresa tu contraseña")
        String contrasena) {
}
