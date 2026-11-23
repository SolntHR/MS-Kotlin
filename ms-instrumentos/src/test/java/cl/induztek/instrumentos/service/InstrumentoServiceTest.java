package cl.induztek.instrumentos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.induztek.instrumentos.dto.CalibracionRequest;
import cl.induztek.instrumentos.dto.InstrumentoRequest;
import cl.induztek.instrumentos.dto.InstrumentoResponse;
import cl.induztek.instrumentos.dto.VigenciaResponse;
import cl.induztek.instrumentos.exception.ConflictoException;
import cl.induztek.instrumentos.exception.RecursoNoEncontradoException;
import cl.induztek.instrumentos.exception.SolicitudInvalidaException;
import cl.induztek.instrumentos.model.Calibracion;
import cl.induztek.instrumentos.model.EstadoCalibracion;
import cl.induztek.instrumentos.model.Instrumento;
import cl.induztek.instrumentos.repository.CalibracionRepository;
import cl.induztek.instrumentos.repository.InstrumentoRepository;

@ExtendWith(MockitoExtension.class)
class InstrumentoServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 23);

    @Mock
    private InstrumentoRepository instrumentoRepository;

    @Mock
    private CalibracionRepository calibracionRepository;

    private InstrumentoService instrumentoService;

    private Instrumento megger5;
    private Instrumento megger10;
    private Instrumento ttr;
    private Instrumento nuevo;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-23T15:00:00Z"), ZoneId.of("America/Santiago"));
        instrumentoService = new InstrumentoService(instrumentoRepository, calibracionRepository, reloj);
        megger5 = instrumento(1L, "MIT-0045821");
        megger10 = instrumento(2L, "MIT-0071233");
        ttr = instrumento(3L, "TTR-30044");
        nuevo = instrumento(4L, "NUEVO-1");
    }

    private Instrumento instrumento(Long id, String serie) {
        return Instrumento.builder().id(id).nombreTipo("Megóhmetro").marca("Megger").modelo("MIT").numeroSerie(serie)
                .build();
    }

    private Calibracion calibracion(Instrumento instrumento, LocalDate desde, LocalDate hasta) {
        return Calibracion.builder().id(instrumento.getId()).instrumento(instrumento).fechaCalibracion(desde)
                .fechaVencimiento(hasta).build();
    }

    @Test
    void calculaElEstadoDeCadaInstrumentoYOrdenaPorVencimiento() {
        when(instrumentoRepository.findAll()).thenReturn(List.of(megger5, megger10, ttr, nuevo));
        when(calibracionRepository.ultimasCalibraciones()).thenReturn(List.of(
                calibracion(megger5, HOY.minusMonths(8), HOY.plusDays(100)),
                calibracion(megger10, HOY.minusYears(1), HOY.minusDays(44)),
                calibracion(ttr, HOY.minusMonths(11), HOY.plusDays(30))));

        List<InstrumentoResponse> lista = instrumentoService.listar(null);

        assertThat(lista).extracting(InstrumentoResponse::id).containsExactly(4L, 2L, 3L, 1L);
        assertThat(lista).extracting(InstrumentoResponse::estado)
                .containsExactly("SIN_CALIBRACION", "VENCIDA", "POR_VENCER", "VIGENTE");
        assertThat(lista.get(1).diasRestantes()).isEqualTo(-44);
        assertThat(lista.get(2).estadoEtiqueta()).isEqualTo("Por vencer");
        assertThat(instrumentoService.listar(EstadoCalibracion.VENCIDA)).extracting(InstrumentoResponse::id)
                .containsExactly(2L);
    }

    @Test
    void obtenerInstrumentoExistenteEInexistente() {
        when(instrumentoRepository.findById(1L)).thenReturn(Optional.of(megger5));
        when(calibracionRepository.findFirstByInstrumentoIdOrderByFechaVencimientoDesc(1L))
                .thenReturn(Optional.of(calibracion(megger5, HOY.minusMonths(8), HOY.plusDays(100))));
        when(instrumentoRepository.findById(9L)).thenReturn(Optional.empty());

        assertThat(instrumentoService.obtener(1L).estado()).isEqualTo("VIGENTE");
        assertThatThrownBy(() -> instrumentoService.obtener(9L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearYActualizarControlanElNumeroDeSerie() {
        when(instrumentoRepository.save(any(Instrumento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(instrumentoRepository.existsByNumeroSerieIgnoreCase("FLK-1")).thenReturn(false);
        when(instrumentoRepository.existsByNumeroSerieIgnoreCase("MIT-0045821")).thenReturn(true);
        when(instrumentoRepository.findById(1L)).thenReturn(Optional.of(megger5));
        when(instrumentoRepository.existsByNumeroSerieIgnoreCaseAndIdNot("MIT-0045821", 1L)).thenReturn(false);
        when(instrumentoRepository.existsByNumeroSerieIgnoreCaseAndIdNot("TTR-30044", 1L)).thenReturn(true);

        InstrumentoResponse creado = instrumentoService.crear(
                new InstrumentoRequest(" Pinza amperimétrica ", "Fluke", "376", " FLK-1 "));
        assertThat(creado.numeroSerie()).isEqualTo("FLK-1");
        assertThat(creado.estado()).isEqualTo("SIN_CALIBRACION");

        assertThatThrownBy(() -> instrumentoService.crear(new InstrumentoRequest("X", "Y", "Z", "MIT-0045821")))
                .isInstanceOf(ConflictoException.class);
        assertThat(instrumentoService.actualizar(1L, new InstrumentoRequest("Megóhmetro 5 kV", "Megger", "MIT525",
                "MIT-0045821")).nombreTipo()).isEqualTo("Megóhmetro 5 kV");
        assertThatThrownBy(() -> instrumentoService.actualizar(1L, new InstrumentoRequest("X", "Y", "Z", "TTR-30044")))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void registraUnaCalibracionValida() {
        when(instrumentoRepository.findById(2L)).thenReturn(Optional.of(megger10));
        when(calibracionRepository.findFirstByInstrumentoIdOrderByFechaVencimientoDesc(2L))
                .thenReturn(Optional.of(calibracion(megger10, LocalDate.of(2025, 10, 10), LocalDate.of(2026, 10, 10))));
        when(calibracionRepository.save(any(Calibracion.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(instrumentoService.registrarCalibracion(2L,
                new CalibracionRequest(HOY, HOY.plusYears(1), " CERT-991 ")).certificado()).isEqualTo("CERT-991");
    }

    @Test
    void rechazaCalibracionesConFechasIncoherentes() {
        when(instrumentoRepository.findById(2L)).thenReturn(Optional.of(megger10));

        assertThatThrownBy(() -> instrumentoService.registrarCalibracion(2L,
                new CalibracionRequest(HOY.plusDays(1), HOY.plusYears(1), null)))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("futura");
        assertThatThrownBy(() -> instrumentoService.registrarCalibracion(2L,
                new CalibracionRequest(HOY, HOY, null)))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("posterior");

        when(calibracionRepository.findFirstByInstrumentoIdOrderByFechaVencimientoDesc(2L))
                .thenReturn(Optional.of(calibracion(megger10, LocalDate.of(2025, 10, 10), LocalDate.of(2026, 10, 10))));
        assertThatThrownBy(() -> instrumentoService.registrarCalibracion(2L,
                new CalibracionRequest(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1), "")))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("anterior");
        verify(calibracionRepository, never()).save(any());
    }

    @Test
    void historialYVigenciaEnUnaFecha() {
        when(instrumentoRepository.findById(1L)).thenReturn(Optional.of(megger5));
        when(calibracionRepository.findByInstrumentoIdOrderByFechaCalibracionDesc(1L))
                .thenReturn(List.of(calibracion(megger5, LocalDate.of(2026, 3, 15), LocalDate.of(2027, 3, 15))));
        when(calibracionRepository.findFirstByInstrumentoIdOrderByFechaVencimientoDesc(1L))
                .thenReturn(Optional.of(calibracion(megger5, LocalDate.of(2026, 3, 15), LocalDate.of(2027, 3, 15))));
        when(calibracionRepository.vigenteEn(1L, LocalDate.of(2026, 9, 26))).thenReturn(true);
        when(calibracionRepository.vigenteEn(1L, HOY)).thenReturn(true);

        assertThat(instrumentoService.calibraciones(1L)).hasSize(1);
        VigenciaResponse vigencia = instrumentoService.vigencia(1L, LocalDate.of(2026, 9, 26));
        assertThat(vigencia.vigente()).isTrue();
        assertThat(vigencia.fechaVencimiento()).isEqualTo(LocalDate.of(2027, 3, 15));
        assertThat(instrumentoService.vigencia(1L, null).fecha()).isEqualTo(HOY);
    }

    @Test
    void elEstadoSeCalculaSegunLosDiasRestantes() {
        assertThat(EstadoCalibracion.segun(null)).isEqualTo(EstadoCalibracion.SIN_CALIBRACION);
        assertThat(EstadoCalibracion.segun(-1L)).isEqualTo(EstadoCalibracion.VENCIDA);
        assertThat(EstadoCalibracion.segun(0L)).isEqualTo(EstadoCalibracion.POR_VENCER);
        assertThat(EstadoCalibracion.segun(31L)).isEqualTo(EstadoCalibracion.VIGENTE);
    }
}
