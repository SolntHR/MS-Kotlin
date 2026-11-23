package cl.induztek.instrumentos.dto;

import java.time.LocalDate;

public record VigenciaResponse(Long instrumentoId, LocalDate fecha, boolean vigente, LocalDate fechaVencimiento) {
}
