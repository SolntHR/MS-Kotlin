package cl.induztek.instalaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.induztek.instalaciones.dto.EquipoRequest;
import cl.induztek.instalaciones.dto.EquipoResponse;
import cl.induztek.instalaciones.exception.ConflictoException;
import cl.induztek.instalaciones.exception.RecursoNoEncontradoException;
import cl.induztek.instalaciones.model.Clasificacion;
import cl.induztek.instalaciones.model.Equipo;
import cl.induztek.instalaciones.model.Instalacion;
import cl.induztek.instalaciones.model.TipoEquipo;
import cl.induztek.instalaciones.repository.EquipoRepository;
import cl.induztek.instalaciones.repository.InstalacionRepository;
import cl.induztek.instalaciones.repository.TipoEquipoRepository;

@ExtendWith(MockitoExtension.class)
class EquipoServiceTest {

    @Mock
    private EquipoRepository equipoRepository;

    @Mock
    private InstalacionRepository instalacionRepository;

    @Mock
    private TipoEquipoRepository tipoEquipoRepository;

    @InjectMocks
    private EquipoService equipoService;

    private Instalacion planta;
    private TipoEquipo transformador;
    private Equipo tr01;

    @BeforeEach
    void preparar() {
        planta = Instalacion.builder().id(1L).clienteId(8L).nombre("Planta Lampa").build();
        transformador = TipoEquipo.builder().id(1L).nombre("Transformador").clasificacion(Clasificacion.PRIMARIO).build();
        tr01 = Equipo.builder().id(1L).instalacion(planta).tipoEquipo(transformador).tag("TR-01").marca("Rhona").build();
    }

    private EquipoRequest request(String tag) {
        return new EquipoRequest(1L, tag, 1L, " Rhona ", "RH-1", "RH-1", 2015, "", 1500.0, 12.0, 72.0, null);
    }

    @Test
    void listarYObtenerDevuelvenLosDatosDelTipoYLaInstalacion() {
        when(equipoRepository.buscar(1L, null, null)).thenReturn(List.of(tr01));
        when(equipoRepository.findWithDetalleById(1L)).thenReturn(Optional.of(tr01));

        EquipoResponse equipo = equipoService.listar(1L, null, null).get(0);
        assertThat(equipo.clienteId()).isEqualTo(8L);
        assertThat(equipo.tipoEquipoNombre()).isEqualTo("Transformador");
        assertThat(equipo.clasificacion()).isEqualTo("PRIMARIO");
        assertThat(equipoService.obtener(1L).tag()).isEqualTo("TR-01");
    }

    @Test
    void obtenerEquipoInexistenteLanza404() {
        when(equipoRepository.findWithDetalleById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipoService.obtener(5L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearGuardaElTagEnMayusculas() {
        when(instalacionRepository.findById(1L)).thenReturn(Optional.of(planta));
        when(tipoEquipoRepository.findById(1L)).thenReturn(Optional.of(transformador));
        when(equipoRepository.save(any(Equipo.class))).thenAnswer(inv -> inv.getArgument(0));

        EquipoResponse respuesta = equipoService.crear(request(" tr-03 "));

        assertThat(respuesta.tag()).isEqualTo("TR-03");
        assertThat(respuesta.marca()).isEqualTo("Rhona");
        assertThat(respuesta.observaciones()).isNull();
        assertThat(respuesta.potenciaKva()).isEqualTo(1500.0);
    }

    @Test
    void elTagNoSePuedeRepetirEnLaMismaInstalacion() {
        when(instalacionRepository.findById(1L)).thenReturn(Optional.of(planta));
        when(equipoRepository.existsByInstalacionIdAndTagIgnoreCase(1L, "TR-01")).thenReturn(true);

        assertThatThrownBy(() -> equipoService.crear(request("tr-01"))).isInstanceOf(ConflictoException.class)
                .hasMessageContaining("TR-01");
    }

    @Test
    void crearConInstalacionOTipoInexistenteLanza404() {
        when(instalacionRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> equipoService.crear(request("TR-09"))).isInstanceOf(RecursoNoEncontradoException.class);

        when(instalacionRepository.findById(1L)).thenReturn(Optional.of(planta));
        when(tipoEquipoRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> equipoService.crear(request("TR-09"))).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void actualizarModificaElEquipoYControlaDuplicados() {
        when(equipoRepository.findWithDetalleById(1L)).thenReturn(Optional.of(tr01));
        when(instalacionRepository.findById(1L)).thenReturn(Optional.of(planta));
        when(tipoEquipoRepository.findById(1L)).thenReturn(Optional.of(transformador));
        when(equipoRepository.save(any(Equipo.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(equipoService.actualizar(1L, request("TR-01")).anio()).isEqualTo(2015);

        when(equipoRepository.existsByInstalacionIdAndTagIgnoreCaseAndIdNot(1L, "TR-02", 1L)).thenReturn(true);
        assertThatThrownBy(() -> equipoService.actualizar(1L, request("TR-02"))).isInstanceOf(ConflictoException.class);
    }
}
