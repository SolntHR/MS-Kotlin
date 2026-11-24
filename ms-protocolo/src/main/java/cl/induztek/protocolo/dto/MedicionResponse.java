package cl.induztek.protocolo.dto;

import cl.induztek.protocolo.model.Medicion;

public record MedicionResponse(Long parametroId, String parametroCodigo, String parametroNombre, Double valor,
        String unidad) {

    public static MedicionResponse desde(Medicion medicion) {
        return new MedicionResponse(medicion.getParametro().getId(), medicion.getParametro().getCodigo(),
                medicion.getParametro().getNombre(), medicion.getValor(), medicion.getUnidad());
    }
}
