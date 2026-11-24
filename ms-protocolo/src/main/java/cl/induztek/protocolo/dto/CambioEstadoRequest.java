package cl.induztek.protocolo.dto;

import cl.induztek.protocolo.model.EstadoProtocolo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CambioEstadoRequest(
        @NotNull(message = "Falta el usuario que revisa")
        Long usuarioId,

        @NotNull(message = "Selecciona el nuevo estado")
        EstadoProtocolo estado,

        @Size(max = 255, message = "El motivo admite hasta 255 caracteres")
        String motivo) {
}
