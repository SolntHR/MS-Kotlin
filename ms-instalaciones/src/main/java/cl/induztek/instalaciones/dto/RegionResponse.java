package cl.induztek.instalaciones.dto;

import cl.induztek.instalaciones.model.Region;

public record RegionResponse(Long id, String nombre) {

    public static RegionResponse desde(Region region) {
        return new RegionResponse(region.getId(), region.getNombre());
    }
}
