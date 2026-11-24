package cl.induztek.protocolo.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProtocoloDetalleResponse(
        String id,
        String numero,
        Long visitaId,
        String numeroOt,
        String tipoVisita,
        Long equipoId,
        String equipoTag,
        String tipoEquipoNombre,
        String instalacionNombre,
        String clienteNombre,
        Long usuarioRegistraId,
        String usuarioRegistraNombre,
        LocalDateTime fechaHoraPrueba,
        Double temperaturaC,
        Double humedadPct,
        String descripcionMantenimiento,
        String observaciones,
        String estado,
        String estadoEtiqueta,
        String protocoloReemplazadoId,
        List<PruebaResponse> pruebas,
        List<HistorialResponse> historial,
        List<EvidenciaResponse> evidencias) {
}
