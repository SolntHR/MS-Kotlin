package cl.induztek.instalaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.induztek.instalaciones.client.UsuarioClient;
import cl.induztek.instalaciones.dto.ClienteResumen;
import cl.induztek.instalaciones.dto.InstalacionRequest;
import cl.induztek.instalaciones.dto.InstalacionResponse;
import cl.induztek.instalaciones.exception.ConflictoException;
import cl.induztek.instalaciones.exception.RecursoNoEncontradoException;
import cl.induztek.instalaciones.exception.SolicitudInvalidaException;
import cl.induztek.instalaciones.model.Comuna;
import cl.induztek.instalaciones.model.Instalacion;
import cl.induztek.instalaciones.model.Region;
import cl.induztek.instalaciones.repository.ComunaRepository;
import cl.induztek.instalaciones.repository.InstalacionRepository;

@ExtendWith(MockitoExtension.class)
class InstalacionServiceTest {

    @Mock
    private InstalacionRepository instalacionRepository;

    @Mock
    private ComunaRepository comunaRepository;

    @Mock
    private UsuarioClient usuarioClient;

    @InjectMocks
    private InstalacionService instalacionService;

    private Comuna lampa;
    private Instalacion planta;
    private final ClienteResumen envasadora = new ClienteResumen(8L, "Envasadora del Pacífico S.A.", "CLIENTE", true);

    @BeforeEach
    void preparar() {
        lampa = Comuna.builder().id(1L).nombre("Lampa")
                .region(Region.builder().id(1L).nombre("Región Metropolitana").build()).build();
        planta = Instalacion.builder().id(1L).clienteId(8L).nombre("Planta Lampa").comuna(lampa).build();
    }

    @Test
    void listarAgregaElNombreDelClienteObtenidoDeMsUsuarios() {
        when(instalacionRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(planta));
        when(instalacionRepository.findByClienteIdOrderByNombreAsc(8L)).thenReturn(List.of(planta));
        when(usuarioClient.nombresClientes()).thenReturn(Map.of(8L, "Envasadora del Pacífico S.A."));

        assertThat(instalacionService.listar(null)).extracting(InstalacionResponse::clienteNombre)
                .containsExactly("Envasadora del Pacífico S.A.");
        assertThat(instalacionService.listar(8L).get(0).regionNombre()).isEqualTo("Región Metropolitana");
    }

    @Test
    void obtenerUnaInstalacionExistenteOInexistente() {
        when(instalacionRepository.findById(1L)).thenReturn(Optional.of(planta));
        when(instalacionRepository.findById(9L)).thenReturn(Optional.empty());
        when(usuarioClient.nombreCliente(8L)).thenReturn("Envasadora del Pacífico S.A.");

        assertThat(instalacionService.obtener(1L).comunaNombre()).isEqualTo("Lampa");
        assertThatThrownBy(() -> instalacionService.obtener(9L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearValidaElClienteYGuardaLaInstalacion() {
        when(usuarioClient.obtenerCliente(8L)).thenReturn(envasadora);
        when(comunaRepository.findById(1L)).thenReturn(Optional.of(lampa));
        when(instalacionRepository.save(any(Instalacion.class))).thenAnswer(inv -> inv.getArgument(0));

        InstalacionResponse respuesta = instalacionService.crear(
                new InstalacionRequest(8L, "  Bodega Norte ", " ", 1L, "Usar casco"));

        assertThat(respuesta.nombre()).isEqualTo("Bodega Norte");
        assertThat(respuesta.ubicacion()).isNull();
        assertThat(respuesta.clienteNombre()).isEqualTo("Envasadora del Pacífico S.A.");
    }

    @Test
    void noPermiteNombresRepetidosParaElMismoCliente() {
        when(usuarioClient.obtenerCliente(8L)).thenReturn(envasadora);
        when(instalacionRepository.existsByClienteIdAndNombreIgnoreCase(8L, "Planta Lampa")).thenReturn(true);

        assertThatThrownBy(() -> instalacionService.crear(new InstalacionRequest(8L, "Planta Lampa", null, 1L, null)))
                .isInstanceOf(ConflictoException.class);
        verify(instalacionRepository, never()).save(any());
    }

    @Test
    void rechazaUsuariosQueNoSonClientesOEstanDesactivados() {
        when(usuarioClient.obtenerCliente(1L)).thenReturn(new ClienteResumen(1L, "Matías", "TECNICO", true));
        when(usuarioClient.obtenerCliente(11L)).thenReturn(new ClienteResumen(11L, "Clínica", "CLIENTE", false));

        assertThatThrownBy(() -> instalacionService.crear(new InstalacionRequest(1L, "X", null, 1L, null)))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("no es un cliente");
        assertThatThrownBy(() -> instalacionService.crear(new InstalacionRequest(11L, "X", null, 1L, null)))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("desactivada");
    }

    @Test
    void rechazaComunaInexistente() {
        when(usuarioClient.obtenerCliente(8L)).thenReturn(envasadora);
        when(comunaRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> instalacionService.crear(new InstalacionRequest(8L, "Bodega", null, 77L, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void actualizarCambiaLosDatosYControlaDuplicados() {
        when(instalacionRepository.findById(1L)).thenReturn(Optional.of(planta));
        when(usuarioClient.obtenerCliente(8L)).thenReturn(envasadora);
        when(comunaRepository.findById(1L)).thenReturn(Optional.of(lampa));
        when(instalacionRepository.save(any(Instalacion.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(instalacionService.actualizar(1L,
                new InstalacionRequest(8L, "Planta Lampa 2", "Km 12", 1L, "Inducción")).ubicacion()).isEqualTo("Km 12");

        when(instalacionRepository.existsByClienteIdAndNombreIgnoreCaseAndIdNot(8L, "Otra", 1L)).thenReturn(true);
        assertThatThrownBy(() -> instalacionService.actualizar(1L, new InstalacionRequest(8L, "Otra", null, 1L, null)))
                .isInstanceOf(ConflictoException.class);
    }
}
