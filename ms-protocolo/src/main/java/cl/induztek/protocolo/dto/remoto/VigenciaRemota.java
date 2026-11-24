package cl.induztek.protocolo.dto.remoto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VigenciaRemota(Long instrumentoId, LocalDate fecha, boolean vigente, LocalDate fechaVencimiento) {
}
