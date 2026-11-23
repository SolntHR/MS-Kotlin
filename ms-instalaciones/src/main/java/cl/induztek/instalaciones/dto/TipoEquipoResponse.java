package cl.induztek.instalaciones.dto;

import cl.induztek.instalaciones.model.TipoEquipo;

public record TipoEquipoResponse(Long id, String nombre, String clasificacion) {

    public static TipoEquipoResponse desde(TipoEquipo tipo) {
        return new TipoEquipoResponse(tipo.getId(), tipo.getNombre(), tipo.getClasificacion().name());
    }
}
