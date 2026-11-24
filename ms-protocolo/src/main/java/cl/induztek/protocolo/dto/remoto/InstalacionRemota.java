package cl.induztek.protocolo.dto.remoto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InstalacionRemota(Long id, Long clienteId, String clienteNombre, String nombre) {
}
