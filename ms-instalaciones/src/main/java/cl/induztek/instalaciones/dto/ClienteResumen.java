package cl.induztek.instalaciones.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClienteResumen(Long id, String nombre, String rolCodigo, boolean activo) {
}
