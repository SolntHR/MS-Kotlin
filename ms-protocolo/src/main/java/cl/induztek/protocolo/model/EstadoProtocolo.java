package cl.induztek.protocolo.model;

public enum EstadoProtocolo {
    BORRADOR("Borrador"),
    REGISTRADO_TERRENO("Registrado en terreno"),
    OBSERVADO("Observado"),
    FORMALIZADO("Formalizado"),
    ANULADO("Anulado");

    private final String etiqueta;

    EstadoProtocolo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean editable() {
        return this == BORRADOR || this == OBSERVADO;
    }

    public boolean vigente() {
        return this != BORRADOR && this != ANULADO;
    }
}
