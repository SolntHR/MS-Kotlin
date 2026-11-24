package cl.induztek.protocolo.dto.remoto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UsuarioRemoto(Long id, String nombre, String rolCodigo, boolean activo) {

    public boolean es(String... roles) {
        for (String rol : roles) {
            if (rol.equals(rolCodigo)) {
                return true;
            }
        }
        return false;
    }
}
