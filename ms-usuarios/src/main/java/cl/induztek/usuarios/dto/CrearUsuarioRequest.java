package cl.induztek.usuarios.dto;

import cl.induztek.usuarios.model.Rol;
import cl.induztek.usuarios.validacion.RutValido;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearUsuarioRequest(
        @NotNull(message = "Elige el perfil del usuario")
        Rol rol,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
        String nombre,

        @NotBlank(message = "El RUT es obligatorio")
        @RutValido
        String rut,

        @NotBlank(message = "El correo personal es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 100, message = "El correo no puede superar los 100 caracteres")
        String correoPersonal,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^\\+56\\s?9\\s?\\d{4}\\s?\\d{4}$", message = "Usa el formato +56 9 XXXX XXXX")
        String telefono,

        @Size(max = 100, message = "El giro no puede superar los 100 caracteres")
        String giro,

        @Size(max = 100, message = "El nombre de contacto no puede superar los 100 caracteres")
        String contactoNombre,

        @Size(max = 100, message = "El cargo no puede superar los 100 caracteres")
        String contactoCargo,

        @NotBlank(message = "Elige una pregunta de seguridad")
        String preguntaSeguridad,

        @NotBlank(message = "Escribe la respuesta de seguridad")
        @Size(max = 100, message = "La respuesta no puede superar los 100 caracteres")
        String respuestaSeguridad) {
}
