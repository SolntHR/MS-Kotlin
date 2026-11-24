package cl.induztek.protocolo.client;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import cl.induztek.protocolo.dto.remoto.UsuarioRemoto;

@Component
public class UsuarioClient extends ClienteRemoto {

    private static final ParameterizedTypeReference<List<UsuarioRemoto>> LISTA = new ParameterizedTypeReference<>() {
    };

    public UsuarioClient(WebClient.Builder builder, @Value("${servicios.usuarios.url}") String url) {
        super(builder, url, "de usuarios");
    }

    public UsuarioRemoto usuario(Long id) {
        return obtener(cliente -> cliente.get().uri("/usuarios/{id}", id).retrieve().bodyToMono(UsuarioRemoto.class),
                "No existe el usuario " + id);
    }

    public Map<Long, String> nombres() {
        List<UsuarioRemoto> usuarios = obtenerOpcional(
                cliente -> cliente.get().uri("/usuarios").retrieve().bodyToMono(LISTA), List.of());
        return usuarios.stream().collect(Collectors.toMap(UsuarioRemoto::id, UsuarioRemoto::nombre, (a, b) -> a));
    }
}
