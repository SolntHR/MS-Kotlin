package cl.induztek.protocolo.dto;

import java.util.Map;

public record IndicadoresResponse(long total, Map<String, Long> porEstado, Double promedioDiasFormalizacion) {
}
