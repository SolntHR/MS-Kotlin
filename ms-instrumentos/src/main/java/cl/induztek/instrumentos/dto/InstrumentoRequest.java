package cl.induztek.instrumentos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InstrumentoRequest(
        @NotBlank(message = "Ingresa el tipo de instrumento")
        @Size(max = 80, message = "El tipo admite hasta 80 caracteres")
        String nombreTipo,

        @NotBlank(message = "Ingresa la marca")
        @Size(max = 60, message = "La marca admite hasta 60 caracteres")
        String marca,

        @NotBlank(message = "Ingresa el modelo")
        @Size(max = 60, message = "El modelo admite hasta 60 caracteres")
        String modelo,

        @NotBlank(message = "Ingresa el número de serie")
        @Size(max = 60, message = "El número de serie admite hasta 60 caracteres")
        String numeroSerie) {
}
