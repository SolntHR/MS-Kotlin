package cl.induztek.protocolo.dto;

import java.util.List;

import cl.induztek.protocolo.model.TipoPrueba;

public record TipoPruebaResponse(Long id, String codigo, String nombre, List<ParametroResponse> parametros) {

    public static TipoPruebaResponse desde(TipoPrueba tipo) {
        return new TipoPruebaResponse(tipo.getId(), tipo.getCodigo(), tipo.getNombre(),
                tipo.getParametros().stream().map(ParametroResponse::desde).toList());
    }
}
