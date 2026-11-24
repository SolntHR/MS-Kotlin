package cl.induztek.protocolo.dto.remoto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EquipoRemoto(
        Long id,
        Long instalacionId,
        String instalacionNombre,
        Long clienteId,
        String tag,
        Long tipoEquipoId,
        String tipoEquipoNombre,
        String marca,
        String numeroSerie,
        Double potenciaKva,
        Double tensionKv,
        Double corrienteA,
        Double maxContactosUohm) {
}
