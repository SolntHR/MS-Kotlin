package cl.induztek.protocolo.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RegistroIncompletoException extends RuntimeException {

    private final transient Map<String, String> campos;

    public RegistroIncompletoException(Map<String, String> campos) {
        super("Faltan datos para registrar el protocolo. Revisa los campos marcados.");
        this.campos = Collections.unmodifiableMap(new LinkedHashMap<>(campos));
    }

    public Map<String, String> getCampos() {
        return campos;
    }
}
