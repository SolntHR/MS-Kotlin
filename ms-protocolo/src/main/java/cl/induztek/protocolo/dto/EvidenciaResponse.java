package cl.induztek.protocolo.dto;

import java.time.LocalDateTime;

import cl.induztek.protocolo.model.Evidencia;

public record EvidenciaResponse(Long id, String nombreOriginal, String tipoContenido, long tamano, String descripcion,
        Long usuarioId, LocalDateTime fechaHora, String url) {

    public static EvidenciaResponse desde(Evidencia evidencia) {
        return new EvidenciaResponse(evidencia.getId(), evidencia.getNombreOriginal(), evidencia.getTipoContenido(),
                evidencia.getTamano(), evidencia.getDescripcion(), evidencia.getUsuarioId(), evidencia.getFechaHora(),
                "/evidencias/" + evidencia.getId() + "/archivo");
    }
}
