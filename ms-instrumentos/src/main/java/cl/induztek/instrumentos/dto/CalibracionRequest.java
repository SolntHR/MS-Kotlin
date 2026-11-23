package cl.induztek.instrumentos.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CalibracionRequest(
        @NotNull(message = "Ingresa la fecha de calibración")
        LocalDate fechaCalibracion,

        @NotNull(message = "Ingresa la fecha de vencimiento")
        LocalDate fechaVencimiento,

        @Size(max = 60, message = "El certificado admite hasta 60 caracteres")
        String certificado) {
}
