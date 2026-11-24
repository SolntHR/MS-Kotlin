package cl.induztek.protocolo.dto;

import java.util.List;

public record ActaResponse(VisitaResponse visita, RecepcionResponse recepcion, List<ProtocoloDetalleResponse> protocolos) {
}
