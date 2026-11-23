package cl.induztek.instalaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InstalacionRequest(
        @NotNull(message = "Selecciona el cliente")
        Long clienteId,

        @NotBlank(message = "Ingresa el nombre de la instalación")
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
        String nombre,

        @Size(max = 150, message = "La ubicación admite hasta 150 caracteres")
        String ubicacion,

        @NotNull(message = "Selecciona la comuna")
        Long comunaId,

        @Size(max = 255, message = "Las observaciones admiten hasta 255 caracteres")
        String observacionesAcceso) {
}
