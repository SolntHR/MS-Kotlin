package cl.induztek.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RestablecerConPreguntaRequest(
        @NotBlank(message = "Ingresa tu correo electrónico")
        @Email(message = "El correo no tiene un formato válido")
        String correo,

        @NotBlank(message = "Selecciona tu pregunta")
        String pregunta,

        @NotBlank(message = "Ingresa tu respuesta")
        String respuesta,

        @NotBlank(message = "Escribe la nueva contraseña")
        @Pattern(regexp = "^[0-9a-f]{64}$", message = "La contraseña debe llegar cifrada con SHA-256 desde la app")
        String nuevaContrasena) {
}
