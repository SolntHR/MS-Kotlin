package cl.induztek.protocolo.dto;

import jakarta.validation.constraints.NotNull;

public record MedicionRequest(
        @NotNull(message = "Falta el parámetro")
        Long parametroId,

        @NotNull(message = "Ingresa el valor")
        Double valor) {
}
