package cl.induztek.protocolo.client;

import java.time.Duration;
import java.util.function.Function;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import cl.induztek.protocolo.exception.RecursoNoEncontradoException;
import cl.induztek.protocolo.exception.ServicioNoDisponibleException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public abstract class ClienteRemoto {

    private static final Duration ESPERA = Duration.ofSeconds(5);

    private final WebClient webClient;
    private final String servicio;

    protected ClienteRemoto(WebClient.Builder builder, String url, String servicio) {
        this.webClient = builder.baseUrl(url).build();
        this.servicio = servicio;
    }

    protected <T> T obtener(Function<WebClient, Mono<T>> consulta, String mensajeNoEncontrado) {
        T respuesta;
        try {
            respuesta = consulta.apply(webClient).timeout(ESPERA).block();
        } catch (WebClientResponseException.NotFound ex) {
            throw new RecursoNoEncontradoException(mensajeNoEncontrado);
        } catch (RuntimeException ex) {
            log.warn("No se pudo consultar {}: {}", servicio, ex.getMessage());
            throw new ServicioNoDisponibleException("El servicio " + servicio + " no está disponible. Intenta más tarde.");
        }
        if (respuesta == null) {
            throw new RecursoNoEncontradoException(mensajeNoEncontrado);
        }
        return respuesta;
    }

    protected <T> T obtenerOpcional(Function<WebClient, Mono<T>> consulta, T valorPorDefecto) {
        try {
            T respuesta = consulta.apply(webClient).timeout(ESPERA).block();
            return respuesta == null ? valorPorDefecto : respuesta;
        } catch (RuntimeException ex) {
            log.warn("No se pudo consultar {}: {}", servicio, ex.getMessage());
            return valorPorDefecto;
        }
    }
}
