package cl.induztek.instalaciones.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import cl.induztek.instalaciones.dto.ClienteResumen;
import cl.induztek.instalaciones.exception.RecursoNoEncontradoException;
import cl.induztek.instalaciones.exception.ServicioNoDisponibleException;
import reactor.core.publisher.Mono;

class UsuarioClientTest {

    private final List<ClientRequest> peticiones = new ArrayList<>();

    private UsuarioClient cliente(HttpStatus estado, String cuerpo) {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(peticion -> {
            peticiones.add(peticion);
            return Mono.just(ClientResponse.create(estado)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(cuerpo)
                    .build());
        });
        return new UsuarioClient(builder, "http://ms-usuarios");
    }

    @Test
    void obtieneElClienteDesdeMsUsuarios() {
        UsuarioClient usuarioClient = cliente(HttpStatus.OK,
                "{\"id\":8,\"nombre\":\"Envasadora del Pacífico S.A.\",\"rolCodigo\":\"CLIENTE\",\"activo\":true,"
                        + "\"correo\":\"mfuentes@envpacifico.ejemplo\"}");

        ClienteResumen resumen = usuarioClient.obtenerCliente(8L);

        assertThat(resumen.nombre()).isEqualTo("Envasadora del Pacífico S.A.");
        assertThat(resumen.rolCodigo()).isEqualTo("CLIENTE");
        assertThat(peticiones.get(0).url().toString()).isEqualTo("http://ms-usuarios/usuarios/8");
        assertThat(usuarioClient.nombreCliente(8L)).isEqualTo("Envasadora del Pacífico S.A.");
    }

    @Test
    void unUsuarioInexistenteSeTraduceA404() {
        UsuarioClient usuarioClient = cliente(HttpStatus.NOT_FOUND, "{\"mensaje\":\"Usuario no encontrado\"}");

        assertThatThrownBy(() -> usuarioClient.obtenerCliente(99L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThat(usuarioClient.nombreCliente(99L)).isNull();
    }

    @Test
    void siMsUsuariosFallaSeInformaServicioNoDisponible() {
        UsuarioClient usuarioClient = cliente(HttpStatus.INTERNAL_SERVER_ERROR, "{}");

        assertThatThrownBy(() -> usuarioClient.obtenerCliente(8L)).isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void siNoHayConexionSeInformaServicioNoDisponible() {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(peticion -> Mono.error(
                new WebClientRequestException(new java.net.ConnectException("sin conexión"), peticion.method(),
                        peticion.url(), peticion.headers())));
        UsuarioClient usuarioClient = new UsuarioClient(builder, "http://ms-usuarios");

        assertThatThrownBy(() -> usuarioClient.obtenerCliente(8L)).isInstanceOf(ServicioNoDisponibleException.class);
        assertThat(usuarioClient.nombresClientes()).isEmpty();
    }

    @Test
    void obtieneLosNombresDeTodosLosClientes() {
        UsuarioClient usuarioClient = cliente(HttpStatus.OK,
                "[{\"id\":8,\"nombre\":\"Envasadora\",\"rolCodigo\":\"CLIENTE\",\"activo\":true},"
                        + "{\"id\":9,\"nombre\":\"Sanitaria\",\"rolCodigo\":\"CLIENTE\",\"activo\":true}]");

        assertThat(usuarioClient.nombresClientes()).containsEntry(8L, "Envasadora").containsEntry(9L, "Sanitaria");
        assertThat(peticiones.get(0).url().toString()).isEqualTo("http://ms-usuarios/usuarios?rol=CLIENTE");
    }
}
