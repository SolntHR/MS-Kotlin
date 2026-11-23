package cl.induztek.usuarios.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import cl.induztek.usuarios.exception.DemasiadosIntentosException;

@Component
public class LimiteIntentos {

    public static final Duration VENTANA = Duration.ofMinutes(15);
    static final String MENSAJE = "Demasiados intentos. Espera 15 minutos antes de volver a intentarlo.";

    private final Clock reloj;
    private final Map<String, Registro> registros = new HashMap<>();

    public LimiteIntentos(Clock reloj) {
        this.reloj = reloj;
    }

    public synchronized void verificar(String accion, String identidad, int maximo) {
        Instant ahora = reloj.instant();
        registros.values().removeIf(registro -> !registro.hasta().isAfter(ahora));
        String clave = clave(accion, identidad);
        Registro registro = registros.get(clave);
        if (registro == null) {
            registros.put(clave, new Registro(1, ahora.plus(VENTANA)));
            return;
        }
        if (registro.intentos() >= maximo) {
            throw new DemasiadosIntentosException(MENSAJE);
        }
        registros.put(clave, new Registro(registro.intentos() + 1, registro.hasta()));
    }

    public synchronized void reiniciar(String accion, String identidad) {
        registros.remove(clave(accion, identidad));
    }

    private String clave(String accion, String identidad) {
        return accion + ":" + (identidad == null ? "" : identidad.trim().toLowerCase(Locale.ROOT));
    }

    private record Registro(int intentos, Instant hasta) {
    }
}
