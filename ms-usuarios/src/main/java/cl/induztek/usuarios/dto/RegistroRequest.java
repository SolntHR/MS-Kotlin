package cl.induztek.usuarios.dto;

import cl.induztek.usuarios.validacion.RutValido;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "Escribe tu nombre completo")
        @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
        String nombre,

        @NotBlank(message = "Escribe tu RUT")
        @RutValido
        String rut,

        @NotBlank(message = "Escribe tu correo personal")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 100, message = "El correo no puede superar los 100 caracteres")
        String correoPersonal,

        @NotBlank(message = "Escribe tu teléfono")
        @Pattern(regexp = "^\\+56\\s?9\\s?\\d{4}\\s?\\d{4}$", message = "Usa el formato +56 9 XXXX XXXX")
        String telefono,

        @NotBlank(message = "Elige una pregunta de seguridad")
        String preguntaSeguridad,

        @NotBlank(message = "Escribe la respuesta de seguridad")
        @Size(max = 100, message = "La respuesta no puede superar los 100 caracteres")
        String respuestaSeguridad) {
}
