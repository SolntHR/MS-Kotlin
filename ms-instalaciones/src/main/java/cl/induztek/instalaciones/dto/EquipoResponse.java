package cl.induztek.instalaciones.dto;

import cl.induztek.instalaciones.model.Equipo;

public record EquipoResponse(
        Long id,
        Long instalacionId,
        String instalacionNombre,
        Long clienteId,
        String tag,
        Long tipoEquipoId,
        String tipoEquipoNombre,
        String clasificacion,
        String marca,
        String modelo,
        String numeroSerie,
        Integer anio,
        String observaciones,
        Double potenciaKva,
        Double tensionKv,
        Double corrienteA,
        Double maxContactosUohm) {

    public static EquipoResponse desde(Equipo equipo) {
        return new EquipoResponse(equipo.getId(), equipo.getInstalacion().getId(), equipo.getInstalacion().getNombre(),
                equipo.getInstalacion().getClienteId(), equipo.getTag(), equipo.getTipoEquipo().getId(),
                equipo.getTipoEquipo().getNombre(), equipo.getTipoEquipo().getClasificacion().name(),
                equipo.getMarca(), equipo.getModelo(), equipo.getNumeroSerie(), equipo.getAnio(),
                equipo.getObservaciones(), equipo.getPotenciaKva(), equipo.getTensionKv(), equipo.getCorrienteA(),
                equipo.getMaxContactosUohm());
    }
}
