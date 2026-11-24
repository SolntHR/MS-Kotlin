package cl.induztek.protocolo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record VisitaResponse(
        Long id,
        String numeroOt,
        Long instalacionId,
        String instalacionNombre,
        Long clienteId,
        String clienteNombre,
        LocalDate fecha,
        String tipoVisita,
        String tipoVisitaEtiqueta,
        Long ingenieroCargoId,
        String ingenieroNombre,
        List<ParticipanteResponse> participantes,
        boolean cerrada,
        LocalDateTime fechaHoraCierre,
        long totalProtocolos,
        long protocolosRegistrados) {
}
