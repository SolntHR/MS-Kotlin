package cl.induztek.instrumentos.model;

public enum EstadoCalibracion {
    VIGENTE("Vigente"),
    POR_VENCER("Por vencer"),
    VENCIDA("Vencida"),
    SIN_CALIBRACION("Sin calibración");

    public static final long DIAS_AVISO = 30;

    private final String etiqueta;

    EstadoCalibracion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static EstadoCalibracion segun(Long diasRestantes) {
        if (diasRestantes == null) {
            return SIN_CALIBRACION;
        }
        if (diasRestantes < 0) {
            return VENCIDA;
        }
        return diasRestantes <= DIAS_AVISO ? POR_VENCER : VIGENTE;
    }
}
