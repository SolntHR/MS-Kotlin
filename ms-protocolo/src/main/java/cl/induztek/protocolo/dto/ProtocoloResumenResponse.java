package cl.induztek.protocolo.dto;

import java.time.LocalDateTime;

public record ProtocoloResumenResponse(
        String id,
        String numero,
        Long visitaId,
        String numeroOt,
        Long equipoId,
        String equipoTag,
        String tipoEquipoNombre,
        String instalacionNombre,
        String clienteNombre,
        Long usuarioRegistraId,
        String usuarioRegistraNombre,
        LocalDateTime fechaHoraPrueba,
        String estado,
        String estadoEtiqueta,
        String resultado,
        Double lectura1Min,
        Double lectura10Min,
        String protocoloReemplazadoId) {
}
