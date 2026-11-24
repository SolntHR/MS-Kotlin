package cl.induztek.protocolo.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.ConnectException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import cl.induztek.protocolo.exception.RecursoNoEncontradoException;
import cl.induztek.protocolo.exception.ServicioNoDisponibleException;
import reactor.core.publisher.Mono;

class ClientesRemotosTest {

    private final List<String> urls = new ArrayList<>();

    private WebClient.Builder responde(Map<String, String> cuerpos) {
        return WebClient.builder().exchangeFunction(peticion -> {
            urls.add(peticion.url().toString());
            String ruta = peticion.url().getPath();
            String cuerpo = cuerpos.get(ruta);
            HttpStatus estado = cuerpo == null ? HttpStatus.NOT_FOUND : HttpStatus.OK;
            return Mono.just(ClientResponse.create(estado)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(cuerpo == null ? "{}" : cuerpo).build());
        });
    }

    private WebClient.Builder sinConexion() {
        return WebClient.builder().exchangeFunction(peticion -> Mono.<ClientResponse>error(new WebClientRequestException(
                new ConnectException("sin conexión"), peticion.method(), peticion.url(), peticion.headers())));
    }

    @Test
    void usuarioClientObtieneUsuariosYNombres() {
        UsuarioClient cliente = new UsuarioClient(responde(Map.of(
                "/usuarios/1", "{\"id\":1,\"nombre\":\"Matías\",\"rolCodigo\":\"TECNICO\",\"activo\":true,\"rut\":\"x\"}",
                "/usuarios", "[{\"id\":1,\"nombre\":\"Matías\",\"rolCodigo\":\"TECNICO\",\"activo\":true}]")),
                "http://ms-usuarios");

        assertThat(cliente.usuario(1L).es("TECNICO", "INGENIERO")).isTrue();
        assertThat(cliente.usuario(1L).es("CLIENTE")).isFalse();
        assertThat(cliente.nombres()).containsEntry(1L, "Matías");
        assertThatThrownBy(() -> cliente.usuario(9L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThat(urls).contains("http://ms-usuarios/usuarios/1", "http://ms-usuarios/usuarios");
    }

    @Test
    void instalacionClientObtieneInstalacionesYEquipos() {
        String equipo = "{\"id\":1,\"instalacionId\":1,\"tag\":\"TR-01\",\"tipoEquipoId\":1,\"tipoEquipoNombre\":\"Transformador\"}";
        InstalacionClient cliente = new InstalacionClient(responde(Map.of(
                "/instalaciones/1", "{\"id\":1,\"clienteId\":8,\"clienteNombre\":\"Envasadora\",\"nombre\":\"Planta Lampa\"}",
                "/instalaciones", "[{\"id\":1,\"clienteId\":8,\"nombre\":\"Planta Lampa\"}]",
                "/instalaciones/1/equipos", "[" + equipo + "]",
                "/equipos/1", equipo,
                "/equipos", "[" + equipo + "]")), "http://ms-instalaciones");

        assertThat(cliente.instalacion(1L).nombre()).isEqualTo("Planta Lampa");
        assertThat(cliente.equipo(1L).tag()).isEqualTo("TR-01");
        assertThat(cliente.equiposDeInstalacion(1L)).hasSize(1);
        assertThat(cliente.instalaciones()).containsKey(1L);
        assertThat(cliente.equipos().get(1L).tipoEquipoNombre()).isEqualTo("Transformador");
        assertThatThrownBy(() -> cliente.equipo(7L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void unaListaVaciaConEstado204SeLeeComoListaVacia() {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(peticion ->
                Mono.just(ClientResponse.create(HttpStatus.NO_CONTENT).build()));
        InstalacionClient cliente = new InstalacionClient(builder, "http://ms-instalaciones");

        assertThat(cliente.equiposDeInstalacion(2L)).isEmpty();
        assertThat(cliente.equipos()).isEmpty();
    }

    @Test
    void instrumentoClientConsultaLaVigenciaEnUnaFecha() {
        InstrumentoClient cliente = new InstrumentoClient(responde(Map.of("/instrumentos/1/vigencia",
                "{\"instrumentoId\":1,\"fecha\":\"2026-09-26\",\"vigente\":true,\"fechaVencimiento\":\"2027-03-15\"}")),
                "http://ms-instrumentos");

        assertThat(cliente.vigencia(1L, LocalDate.of(2026, 9, 26)).vigente()).isTrue();
        assertThat(urls).contains("http://ms-instrumentos/instrumentos/1/vigencia?fecha=2026-09-26");
    }

    @Test
    void siUnServicioNoRespondeSeInforma503OSeUsaUnValorPorDefecto() {
        UsuarioClient usuarios = new UsuarioClient(sinConexion(), "http://ms-usuarios");
        InstalacionClient instalaciones = new InstalacionClient(sinConexion(), "http://ms-instalaciones");
        InstrumentoClient instrumentos = new InstrumentoClient(sinConexion(), "http://ms-instrumentos");

        assertThatThrownBy(() -> usuarios.usuario(1L)).isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessageContaining("de usuarios");
        assertThatThrownBy(() -> instrumentos.vigencia(1L, LocalDate.now()))
                .isInstanceOf(ServicioNoDisponibleException.class);
        assertThat(usuarios.nombres()).isEmpty();
        assertThat(instalaciones.instalaciones()).isEmpty();
        assertThat(instalaciones.equipos()).isEmpty();
    }
}
