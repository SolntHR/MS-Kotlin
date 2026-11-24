package cl.induztek.protocolo.dto;

import java.time.LocalDateTime;

public record HistorialResponse(String estadoAnterior, String estadoNuevo, Long usuarioId, String usuarioNombre,
        LocalDateTime fechaHora, String motivo) {
}
