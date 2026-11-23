package cl.induztek.instrumentos.dto;

import java.time.LocalDate;

import cl.induztek.instrumentos.model.Calibracion;

public record CalibracionResponse(
        Long id,
        Long instrumentoId,
        LocalDate fechaCalibracion,
        LocalDate fechaVencimiento,
        String certificado) {

    public static CalibracionResponse desde(Calibracion calibracion) {
        return new CalibracionResponse(calibracion.getId(), calibracion.getInstrumento().getId(),
                calibracion.getFechaCalibracion(), calibracion.getFechaVencimiento(), calibracion.getCertificado());
    }
}
