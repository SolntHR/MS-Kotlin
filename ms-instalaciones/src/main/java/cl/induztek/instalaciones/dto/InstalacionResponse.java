package cl.induztek.instalaciones.dto;

import cl.induztek.instalaciones.model.Instalacion;

public record InstalacionResponse(
        Long id,
        Long clienteId,
        String clienteNombre,
        String nombre,
        String ubicacion,
        Long comunaId,
        String comunaNombre,
        String regionNombre,
        String observacionesAcceso) {

    public static InstalacionResponse desde(Instalacion instalacion, String clienteNombre) {
        return new InstalacionResponse(instalacion.getId(), instalacion.getClienteId(), clienteNombre,
                instalacion.getNombre(), instalacion.getUbicacion(), instalacion.getComuna().getId(),
                instalacion.getComuna().getNombre(), instalacion.getComuna().getRegion().getNombre(),
                instalacion.getObservacionesAcceso());
    }
}
