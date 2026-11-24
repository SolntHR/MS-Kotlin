package cl.induztek.protocolo.dto;

import java.time.LocalDateTime;

public record MedicionAnteriorResponse(
        Long tipoPruebaId,
        String elemento,
        String protocoloNumero,
        LocalDateTime fechaHoraPrueba,
        String parametroCodigo,
        Double valor,
        String unidad) {
}
