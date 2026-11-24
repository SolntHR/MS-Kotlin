package cl.induztek.protocolo.dto;

import java.time.LocalDate;
import java.util.List;

import cl.induztek.protocolo.model.TipoVisita;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VisitaRequest(
        @NotNull(message = "Falta el usuario que asigna la visita")
        Long usuarioId,

        @NotBlank(message = "Ingresa el número de OT")
        @Pattern(regexp = "^OT-\\d{4}-\\d{4}$", message = "La OT debe tener el formato OT-2026-0001")
        String numeroOt,

        @NotNull(message = "Selecciona la instalación")
        Long instalacionId,

        @NotNull(message = "Selecciona la fecha")
        LocalDate fecha,

        @NotNull(message = "Selecciona el tipo de visita")
        TipoVisita tipoVisita,

        @NotNull(message = "Selecciona el ingeniero a cargo")
        Long ingenieroCargoId,

        @NotEmpty(message = "Asigna al menos un técnico")
        List<@NotNull Long> participantes) {
}
