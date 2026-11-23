package cl.induztek.instrumentos.dto;

import java.time.LocalDate;

public record InstrumentoResponse(
        Long id,
        String nombreTipo,
        String marca,
        String modelo,
        String numeroSerie,
        LocalDate fechaCalibracion,
        LocalDate fechaVencimiento,
        Long diasRestantes,
        String estado,
        String estadoEtiqueta) {
}
