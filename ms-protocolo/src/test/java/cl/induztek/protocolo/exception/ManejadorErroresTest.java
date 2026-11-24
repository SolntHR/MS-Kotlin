package cl.induztek.protocolo.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class ManejadorErroresTest {

    private final ManejadorErrores manejador = new ManejadorErrores();

    @Test
    void unaRutaInexistenteResponde404YUnMetodoNoPermitido405() {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/no-existe");

        var noEncontrada = manejador.rutaInexistente(
                new NoResourceFoundException(HttpMethod.GET, "/no-existe", "no-existe"), request);
        var noPermitido = manejador.metodoNoPermitido(
                new HttpRequestMethodNotSupportedException("DELETE", Set.of("GET")), request);

        assertThat(noEncontrada.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(noEncontrada.getBody().ruta()).isEqualTo("/no-existe");
        assertThat(noPermitido.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(noPermitido.getBody().mensaje()).contains("DELETE");
    }
}
