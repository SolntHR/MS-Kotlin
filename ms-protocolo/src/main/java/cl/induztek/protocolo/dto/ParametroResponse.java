package cl.induztek.protocolo.dto;

import cl.induztek.protocolo.model.Parametro;

public record ParametroResponse(Long id, String codigo, String nombre, String unidad) {

    public static ParametroResponse desde(Parametro parametro) {
        return new ParametroResponse(parametro.getId(), parametro.getCodigo(), parametro.getNombre(),
                parametro.getUnidad());
    }
}
