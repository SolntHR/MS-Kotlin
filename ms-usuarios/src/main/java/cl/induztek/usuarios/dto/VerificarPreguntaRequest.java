package cl.induztek.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerificarPreguntaRequest(
        @NotBlank(message = "Ingresa tu correo electrónico")
        @Email(message = "El correo no tiene un formato válido")
        String correo,

        @NotBlank(message = "Selecciona tu pregunta")
        String pregunta,

        @NotBlank(message = "Ingresa tu respuesta")
        String respuesta) {
}
