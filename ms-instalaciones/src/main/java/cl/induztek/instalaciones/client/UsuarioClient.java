package cl.induztek.instalaciones.client;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import cl.induztek.instalaciones.dto.ClienteResumen;
import cl.induztek.instalaciones.exception.RecursoNoEncontradoException;
import cl.induztek.instalaciones.exception.ServicioNoDisponibleException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UsuarioClient {

    private static final Duration ESPERA = Duration.ofSeconds(5);
    private static final ParameterizedTypeReference<List<ClienteResumen>> LISTA =
            new ParameterizedTypeReference<>() {
            };

    private final WebClient webClient;

    public UsuarioClient(WebClient.Builder builder, @Value("${servicios.usuarios.url}") String url) {
        this.webClient = builder.baseUrl(url).build();
    }

    public ClienteResumen obtenerCliente(Long id) {
        try {
            ClienteResumen cliente = webClient.get()
                    .uri("/usuarios/{id}", id)
                    .retrieve()
                    .bodyToMono(ClienteResumen.class)
                    .timeout(ESPERA)
                    .block();
            if (cliente == null) {
                throw new RecursoNoEncontradoException("No existe un cliente con id " + id);
            }
            return cliente;
        } catch (WebClientResponseException.NotFound ex) {
            throw new RecursoNoEncontradoException("No existe un cliente con id " + id);
        } catch (RecursoNoEncontradoException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("No se pudo consultar ms-usuarios: {}", ex.getMessage());
            throw new ServicioNoDisponibleException("El servicio de usuarios no está disponible. Intenta más tarde.");
        }
    }

    public Map<Long, String> nombresClientes() {
        try {
            List<ClienteResumen> clientes = webClient.get()
                    .uri(uri -> uri.path("/usuarios").queryParam("rol", "CLIENTE").build())
                    .retrieve()
                    .bodyToMono(LISTA)
                    .timeout(ESPERA)
                    .block();
            if (clientes == null) {
                return Map.of();
            }
            return clientes.stream().collect(Collectors.toMap(ClienteResumen::id, ClienteResumen::nombre,
                    (primero, segundo) -> primero));
        } catch (RuntimeException ex) {
            log.warn("No se pudieron obtener los nombres de los clientes: {}", ex.getMessage());
            return Map.of();
        }
    }

    public String nombreCliente(Long id) {
        try {
            return obtenerCliente(id).nombre();
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
