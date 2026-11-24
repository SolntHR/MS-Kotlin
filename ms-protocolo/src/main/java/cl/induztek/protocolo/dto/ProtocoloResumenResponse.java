package cl.induztek.protocolo.dto;

import java.time.LocalDateTime;

public record ProtocoloResumenResponse(
        String id,
        String numero,
        Long visitaId,
        String numeroOt,
        Long equipoId,
        String equipoTag,
        String instalacionNombre,
        String clienteNombre,
        Long usuarioRegistraId,
        LocalDateTime fechaHoraPrueba,
        String estado,
        String estadoEtiqueta) {
}
