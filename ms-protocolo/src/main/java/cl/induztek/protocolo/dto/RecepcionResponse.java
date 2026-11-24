package cl.induztek.protocolo.dto;

import java.time.LocalDateTime;

import cl.induztek.protocolo.model.RecepcionActa;

public record RecepcionResponse(String nombre, String rut, String cargo, LocalDateTime fechaHora) {

    public static RecepcionResponse desde(RecepcionActa recepcion) {
        if (recepcion == null || recepcion.getNombre() == null) {
            return null;
        }
        return new RecepcionResponse(recepcion.getNombre(), recepcion.getRut(), recepcion.getCargo(),
                recepcion.getFechaHora());
    }
}
