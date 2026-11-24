package cl.induztek.protocolo.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProtocoloRequest(
        @NotNull(message = "Falta el usuario que registra")
        Long usuarioId,

        @NotNull(message = "Selecciona la visita")
        Long visitaId,

        @NotNull(message = "Selecciona el equipo")
        Long equipoId,

        @DecimalMin(value = "-30", message = "La temperatura debe estar entre -30 y 60 °C")
        @DecimalMax(value = "60", message = "La temperatura debe estar entre -30 y 60 °C")
        Double temperaturaC,

        @DecimalMin(value = "0", message = "La humedad debe estar entre 0 y 100 %")
        @DecimalMax(value = "100", message = "La humedad debe estar entre 0 y 100 %")
        Double humedadPct,

        @Size(max = 500, message = "La descripción admite hasta 500 caracteres")
        String descripcionMantenimiento,

        @Size(max = 500, message = "Las observaciones admiten hasta 500 caracteres")
        String observaciones,

        Boolean registrar,

        List<@Valid PruebaRequest> pruebas) {

    public boolean esRegistro() {
        return Boolean.TRUE.equals(registrar);
    }

    public List<PruebaRequest> pruebasOVacio() {
        return pruebas == null ? List.of() : pruebas;
    }
}
