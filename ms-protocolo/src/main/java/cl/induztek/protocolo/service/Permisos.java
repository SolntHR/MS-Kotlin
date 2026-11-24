package cl.induztek.protocolo.service;

import org.springframework.stereotype.Component;

import cl.induztek.protocolo.client.UsuarioClient;
import cl.induztek.protocolo.dto.remoto.UsuarioRemoto;
import cl.induztek.protocolo.exception.AccesoDenegadoException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class Permisos {

    public static final String TECNICO = "TECNICO";
    public static final String INGENIERO = "INGENIERO";
    public static final String JEFE_TECNICO = "JEFE_TECNICO";
    public static final String CLIENTE = "CLIENTE";

    private final UsuarioClient usuarioClient;

    public UsuarioRemoto activo(Long usuarioId) {
        UsuarioRemoto usuario = usuarioClient.usuario(usuarioId);
        if (!usuario.activo()) {
            throw new AccesoDenegadoException("La cuenta del usuario está desactivada");
        }
        return usuario;
    }

    public UsuarioRemoto personal(Long usuarioId) {
        UsuarioRemoto usuario = activo(usuarioId);
        if (!usuario.es(TECNICO, INGENIERO, JEFE_TECNICO)) {
            throw new AccesoDenegadoException("Tu perfil no puede registrar protocolos");
        }
        return usuario;
    }

    public UsuarioRemoto revisor(Long usuarioId) {
        UsuarioRemoto usuario = activo(usuarioId);
        if (!usuario.es(INGENIERO, JEFE_TECNICO)) {
            throw new AccesoDenegadoException("Solo el Ingeniero o el Jefe técnico pueden hacer este cambio");
        }
        return usuario;
    }
}
