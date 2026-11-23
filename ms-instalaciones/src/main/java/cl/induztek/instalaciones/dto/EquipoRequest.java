package cl.induztek.instalaciones.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EquipoRequest(
        @NotNull(message = "Selecciona la instalación")
        Long instalacionId,

        @NotBlank(message = "Ingresa el TAG del equipo")
        @Size(max = 30, message = "El TAG admite hasta 30 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9-]*$", message = "El TAG solo admite letras, números y guiones")
        String tag,

        @NotNull(message = "Selecciona el tipo de equipo")
        Long tipoEquipoId,

        @Size(max = 60, message = "La marca admite hasta 60 caracteres")
        String marca,

        @Size(max = 60, message = "El modelo admite hasta 60 caracteres")
        String modelo,

        @Size(max = 60, message = "El número de serie admite hasta 60 caracteres")
        String numeroSerie,

        @Min(value = 1950, message = "El año debe ser 1950 o posterior")
        @Max(value = 2100, message = "El año no es válido")
        Integer anio,

        @Size(max = 255, message = "Las observaciones admiten hasta 255 caracteres")
        String observaciones,

        @Positive(message = "La potencia debe ser mayor que 0")
        Double potenciaKva,

        @Positive(message = "La tensión debe ser mayor que 0")
        Double tensionKv,

        @Positive(message = "La corriente debe ser mayor que 0")
        Double corrienteA,

        @Positive(message = "El máximo de resistencia de contactos debe ser mayor que 0")
        Double maxContactosUohm) {
}
