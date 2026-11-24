package cl.induztek.protocolo.model;

public enum Resultado {
    APROBADO("Aprobado"),
    OBSERVADO("Observado"),
    RECHAZADO("Rechazado");

    private final String etiqueta;

    Resultado(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
