package cl.induztek.protocolo.dto;

import java.util.List;

import cl.induztek.protocolo.model.Prueba;

public record PruebaResponse(
        Long id,
        Long tipoPruebaId,
        String tipoPruebaCodigo,
        String tipoPruebaNombre,
        String elemento,
        String momento,
        boolean realizada,
        String motivoNoRealizada,
        Long instrumentoId,
        String resultado,
        String comentario,
        boolean ingresadoManualmente,
        List<MedicionResponse> mediciones) {

    public static PruebaResponse desde(Prueba prueba) {
        return new PruebaResponse(prueba.getId(), prueba.getTipoPrueba().getId(), prueba.getTipoPrueba().getCodigo(),
                prueba.getTipoPrueba().getNombre(), prueba.getElemento(), prueba.getMomento(), prueba.isRealizada(),
                prueba.getMotivoNoRealizada(), prueba.getInstrumentoId(),
                prueba.getResultado() == null ? null : prueba.getResultado().name(), prueba.getComentario(),
                prueba.isIngresadoManualmente(), prueba.getMediciones().stream().map(MedicionResponse::desde).toList());
    }
}
