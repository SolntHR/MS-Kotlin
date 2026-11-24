package cl.induztek.protocolo.dto;

import java.util.List;

import cl.induztek.protocolo.model.Resultado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PruebaRequest(
        @NotNull(message = "Selecciona el tipo de prueba")
        Long tipoPruebaId,

        @NotBlank(message = "Indica el elemento probado")
        @Size(max = 40, message = "El elemento admite hasta 40 caracteres")
        String elemento,

        @Size(max = 20, message = "El momento admite hasta 20 caracteres")
        String momento,

        @NotNull(message = "Indica si la prueba se realizó")
        Boolean realizada,

        @Size(max = 255, message = "El motivo admite hasta 255 caracteres")
        String motivoNoRealizada,

        Long instrumentoId,

        Resultado resultado,

        @Size(max = 255, message = "El comentario admite hasta 255 caracteres")
        String comentario,

        Boolean ingresadoManualmente,

        List<@Valid MedicionRequest> mediciones) {

    public boolean fueRealizada() {
        return Boolean.TRUE.equals(realizada);
    }

    public boolean esManual() {
        return Boolean.TRUE.equals(ingresadoManualmente);
    }

    public List<MedicionRequest> medicionesOVacio() {
        return mediciones == null ? List.of() : mediciones;
    }
}
