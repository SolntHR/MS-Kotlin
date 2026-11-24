package cl.induztek.protocolo.model;

public enum TipoVisita {
    SOLO_PRUEBAS("Solo pruebas"),
    PRUEBAS_Y_MANT("Pruebas y mantenimiento");

    private final String etiqueta;

    TipoVisita(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
