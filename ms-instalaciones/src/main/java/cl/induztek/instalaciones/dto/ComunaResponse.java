package cl.induztek.instalaciones.dto;

import cl.induztek.instalaciones.model.Comuna;

public record ComunaResponse(Long id, String nombre, Long regionId, String regionNombre) {

    public static ComunaResponse desde(Comuna comuna) {
        return new ComunaResponse(comuna.getId(), comuna.getNombre(), comuna.getRegion().getId(),
                comuna.getRegion().getNombre());
    }
}
