package cl.induztek.protocolo.client;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import cl.induztek.protocolo.dto.remoto.EquipoRemoto;
import cl.induztek.protocolo.dto.remoto.InstalacionRemota;

@Component
public class InstalacionClient extends ClienteRemoto {

    private static final ParameterizedTypeReference<List<EquipoRemoto>> EQUIPOS = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<List<InstalacionRemota>> INSTALACIONES =
            new ParameterizedTypeReference<>() {
            };

    public InstalacionClient(WebClient.Builder builder, @Value("${servicios.instalaciones.url}") String url) {
        super(builder, url, "de instalaciones");
    }

    public InstalacionRemota instalacion(Long id) {
        return obtener(cliente -> cliente.get().uri("/instalaciones/{id}", id).retrieve()
                .bodyToMono(InstalacionRemota.class), "No existe la instalación " + id);
    }

    public EquipoRemoto equipo(Long id) {
        return obtener(cliente -> cliente.get().uri("/equipos/{id}", id).retrieve().bodyToMono(EquipoRemoto.class),
                "No existe el equipo " + id);
    }

    public List<EquipoRemoto> equiposDeInstalacion(Long instalacionId) {
        return obtener(cliente -> cliente.get().uri("/instalaciones/{id}/equipos", instalacionId).retrieve()
                .bodyToMono(EQUIPOS).defaultIfEmpty(List.of()), "No existe la instalación " + instalacionId);
    }

    public Map<Long, InstalacionRemota> instalaciones() {
        List<InstalacionRemota> lista = obtenerOpcional(
                cliente -> cliente.get().uri("/instalaciones").retrieve().bodyToMono(INSTALACIONES), List.of());
        return lista.stream().collect(Collectors.toMap(InstalacionRemota::id, Function.identity(), (a, b) -> a));
    }

    public Map<Long, EquipoRemoto> equipos() {
        List<EquipoRemoto> lista = obtenerOpcional(
                cliente -> cliente.get().uri("/equipos").retrieve().bodyToMono(EQUIPOS), List.of());
        return lista.stream().collect(Collectors.toMap(EquipoRemoto::id, Function.identity(), (a, b) -> a));
    }
}
