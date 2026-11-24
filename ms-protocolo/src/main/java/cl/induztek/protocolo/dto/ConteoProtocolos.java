package cl.induztek.protocolo.dto;

import cl.induztek.protocolo.model.EstadoProtocolo;

public record ConteoProtocolos(Long visitaId, EstadoProtocolo estado, long cantidad) {
}
