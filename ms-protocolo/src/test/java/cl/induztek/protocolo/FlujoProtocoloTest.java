package cl.induztek.protocolo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import cl.induztek.protocolo.client.InstalacionClient;
import cl.induztek.protocolo.client.InstrumentoClient;
import cl.induztek.protocolo.client.UsuarioClient;
import cl.induztek.protocolo.dto.ActaResponse;
import cl.induztek.protocolo.dto.CambioEstadoRequest;
import cl.induztek.protocolo.dto.EvidenciaResponse;
import cl.induztek.protocolo.dto.MedicionAnteriorResponse;
import cl.induztek.protocolo.dto.MedicionRequest;
import cl.induztek.protocolo.dto.ProtocoloDetalleResponse;
import cl.induztek.protocolo.dto.ProtocoloRequest;
import cl.induztek.protocolo.dto.ProtocoloResumenResponse;
import cl.induztek.protocolo.dto.PruebaRequest;
import cl.induztek.protocolo.dto.RecepcionRequest;
import cl.induztek.protocolo.dto.VisitaRequest;
import cl.induztek.protocolo.dto.VisitaResponse;
import cl.induztek.protocolo.dto.remoto.EquipoRemoto;
import cl.induztek.protocolo.dto.remoto.InstalacionRemota;
import cl.induztek.protocolo.dto.remoto.UsuarioRemoto;
import cl.induztek.protocolo.dto.remoto.VigenciaRemota;
import cl.induztek.protocolo.exception.AccesoDenegadoException;
import cl.induztek.protocolo.exception.ConflictoException;
import cl.induztek.protocolo.exception.RecursoNoEncontradoException;
import cl.induztek.protocolo.exception.RegistroIncompletoException;
import cl.induztek.protocolo.exception.SolicitudInvalidaException;
import cl.induztek.protocolo.model.EstadoProtocolo;
import cl.induztek.protocolo.model.Resultado;
import cl.induztek.protocolo.model.TipoVisita;
import cl.induztek.protocolo.service.CatalogoService;
import cl.induztek.protocolo.service.EvidenciaService;
import cl.induztek.protocolo.service.ProtocoloService;
import cl.induztek.protocolo.service.VisitaService;

@SpringBootTest
@Transactional
class FlujoProtocoloTest {

    private static final Long MATIAS = 1L;
    private static final Long JAVIERA = 2L;
    private static final Long ANDREA = 5L;
    private static final Long RICARDO = 7L;
    private static final Long ENVASADORA = 8L;
    private static final Long CLINICA = 11L;

    @MockitoBean
    private UsuarioClient usuarioClient;

    @MockitoBean
    private InstalacionClient instalacionClient;

    @MockitoBean
    private InstrumentoClient instrumentoClient;

    @Autowired
    private VisitaService visitaService;

    @Autowired
    private ProtocoloService protocoloService;

    @Autowired
    private EvidenciaService evidenciaService;

    @Autowired
    private CatalogoService catalogoService;

    private final Map<Long, UsuarioRemoto> usuarios = Stream.of(
            new UsuarioRemoto(1L, "Matías Godoy Herrera", "TECNICO", true),
            new UsuarioRemoto(2L, "Javiera Tapia Muñoz", "TECNICO", true),
            new UsuarioRemoto(3L, "Carlos Rondón Pérez", "TECNICO", false),
            new UsuarioRemoto(5L, "Andrea Contreras Silva", "INGENIERO", true),
            new UsuarioRemoto(6L, "Sebastián Morales Gutiérrez", "INGENIERO", true),
            new UsuarioRemoto(7L, "Ricardo Espinoza Lagos", "JEFE_TECNICO", true),
            new UsuarioRemoto(8L, "Envasadora del Pacífico S.A.", "CLIENTE", true),
            new UsuarioRemoto(11L, "Clínica Los Aromos Ltda.", "CLIENTE", true))
            .collect(Collectors.toMap(UsuarioRemoto::id, Function.identity()));

    private final Map<Long, EquipoRemoto> equipos = Stream.of(
            equipo(1L, 1L, "TR-01", 1L, "Transformador"),
            equipo(2L, 1L, "TR-02", 1L, "Transformador"),
            equipo(3L, 2L, "TR-A", 1L, "Transformador"),
            equipo(4L, 4L, "CEL-MT-03", 3L, "Celda MT"),
            equipo(5L, 3L, "INT-52-1", 2L, "Interruptor"))
            .collect(Collectors.toMap(EquipoRemoto::id, Function.identity()));

    private final Map<Long, InstalacionRemota> instalaciones = Map.of(
            1L, new InstalacionRemota(1L, 8L, "Envasadora del Pacífico S.A.", "Planta Lampa"),
            2L, new InstalacionRemota(2L, 9L, "Sanitaria Valle Central S.A.", "Planta Elevadora Sur"),
            3L, new InstalacionRemota(3L, 10L, "Minera Cerro Alto SpA", "Faena Til Til"),
            4L, new InstalacionRemota(4L, 11L, "Clínica Los Aromos Ltda.", "Edificio Clínico"));

    private static EquipoRemoto equipo(Long id, Long instalacionId, String tag, Long tipoId, String tipo) {
        return new EquipoRemoto(id, instalacionId, "Instalación " + instalacionId, 8L, tag, tipoId, tipo, "Marca",
                "S-" + id, null, 12.0, null, null);
    }

    @BeforeEach
    void preparar() {
        when(usuarioClient.usuario(anyLong())).thenAnswer(inv -> {
            UsuarioRemoto usuario = usuarios.get(inv.<Long>getArgument(0));
            if (usuario == null) {
                throw new RecursoNoEncontradoException("No existe el usuario");
            }
            return usuario;
        });
        when(usuarioClient.nombres()).thenReturn(usuarios.values().stream()
                .collect(Collectors.toMap(UsuarioRemoto::id, UsuarioRemoto::nombre)));
        when(instalacionClient.equipo(anyLong())).thenAnswer(inv -> equipos.get(inv.<Long>getArgument(0)));
        when(instalacionClient.equipos()).thenReturn(equipos);
        when(instalacionClient.instalaciones()).thenReturn(instalaciones);
        when(instalacionClient.instalacion(anyLong())).thenAnswer(inv -> instalaciones.get(inv.<Long>getArgument(0)));
        when(instalacionClient.equiposDeInstalacion(anyLong())).thenAnswer(inv -> equipos.values().stream()
                .filter(e -> e.instalacionId().equals(inv.getArgument(0))).toList());
        when(instrumentoClient.vigencia(anyLong(), any(LocalDate.class))).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return new VigenciaRemota(id, inv.getArgument(1), id != 2L && id != 4L, LocalDate.of(2027, 1, 1));
        });
    }

    private VisitaResponse visitaDeHoy() {
        return visitaService.listar(MATIAS, true).stream().filter(v -> v.numeroOt().equals("OT-2026-1215"))
                .findFirst().orElseThrow();
    }

    private PruebaRequest aislacion(Long instrumentoId, Resultado resultado) {
        return new PruebaRequest(1L, "AT-T", "UNICA", true, null, instrumentoId, resultado, null, false,
                List.of(new MedicionRequest(1L, 11000.0), new MedicionRequest(2L, 25000.0),
                        new MedicionRequest(3L, 5.0)));
    }

    private ProtocoloRequest request(Long visitaId, Long equipoId, boolean registrar, Double temperatura,
            List<PruebaRequest> pruebas) {
        return new ProtocoloRequest(MATIAS, visitaId, equipoId, temperatura, 55.0, null, "Sin novedades",
                registrar, pruebas);
    }

    @Test
    void cargaLosCatalogosYLasVisitasIniciales() {
        assertThat(catalogoService.tiposPrueba(null)).hasSize(5);
        assertThat(catalogoService.tiposPrueba(1L)).extracting(t -> t.codigo())
                .containsExactly("AISLACION", "TTR", "DEVANADOS");
        assertThat(catalogoService.tiposPrueba(5L).get(0).parametros()).hasSize(4);

        assertThat(visitaService.listar(RICARDO, null)).hasSize(7);
        assertThat(visitaService.listar(MATIAS, true)).extracting(VisitaResponse::numeroOt)
                .contains("OT-2026-1215", "OT-2026-1216", "OT-2026-1187");
        assertThat(visitaService.listar(ENVASADORA, null)).allSatisfy(v -> assertThat(v.cerrada()).isTrue())
                .extracting(VisitaResponse::numeroOt).containsExactlyInAnyOrder("OT-2025-1032", "OT-2024-0951");
        VisitaResponse ot1187 = visitaService.listar(MATIAS, true).stream()
                .filter(v -> v.numeroOt().equals("OT-2026-1187")).findFirst().orElseThrow();
        assertThat(ot1187.totalProtocolos()).isEqualTo(2);
        assertThat(ot1187.instalacionNombre()).isEqualTo("Planta Lampa");
        assertThat(ot1187.ingenieroNombre()).isEqualTo("Andrea Contreras Silva");
    }

    @Test
    void flujoCompletoDesdeElBorradorHastaElActa() {
        VisitaResponse visita = visitaDeHoy();
        assertThat(visitaService.equiposDisponibles(visita.id(), null)).extracting(EquipoRemoto::tag)
                .containsExactlyInAnyOrder("TR-01", "TR-02");

        ProtocoloDetalleResponse borrador = protocoloService.crear(request(visita.id(), 1L, false, null,
                List.of(aislacion(null, null))));
        assertThat(borrador.estado()).isEqualTo("BORRADOR");
        assertThat(borrador.numero()).startsWith("PT-2026-");
        assertThat(visitaService.equiposDisponibles(visita.id(), null)).extracting(EquipoRemoto::tag)
                .containsExactly("TR-02");
        assertThat(visitaService.equiposDisponibles(visita.id(), borrador.id())).hasSize(2);

        assertThatThrownBy(() -> protocoloService.actualizar(borrador.id(),
                request(visita.id(), 1L, true, null, List.of(aislacion(null, null)))))
                .isInstanceOfSatisfying(RegistroIncompletoException.class, ex -> assertThat(ex.getCampos())
                        .containsKeys("temperaturaC", "pruebas[0].instrumentoId", "pruebas[0].resultado"));

        assertThatThrownBy(() -> protocoloService.actualizar(borrador.id(),
                request(visita.id(), 1L, true, 18.0, List.of(aislacion(2L, Resultado.OBSERVADO)))))
                .isInstanceOfSatisfying(RegistroIncompletoException.class, ex -> assertThat(ex.getCampos())
                        .containsEntry("pruebas[0].instrumentoId",
                                "El instrumento no tenía calibración vigente el 23-11-2026")
                        .containsKey("pruebas[0].comentario"));

        assertThatThrownBy(() -> visitaService.registrarRecepcion(visita.id(),
                new RecepcionRequest("Marcela Fuentes Rojas", "14.226.791-8", "Jefa de Mantenimiento")))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("borrador");

        ProtocoloDetalleResponse registrado = protocoloService.actualizar(borrador.id(),
                request(visita.id(), 1L, true, 18.0, List.of(aislacion(1L, Resultado.APROBADO))));
        assertThat(registrado.estado()).isEqualTo("REGISTRADO_TERRENO");
        assertThat(registrado.pruebas().get(0).mediciones()).hasSize(3);
        assertThat(registrado.historial()).extracting(h -> h.estadoNuevo())
                .containsExactly("BORRADOR", "REGISTRADO_TERRENO");

        assertThatThrownBy(() -> protocoloService.cambiarEstado(registrado.id(),
                new CambioEstadoRequest(MATIAS, EstadoProtocolo.FORMALIZADO, null)))
                .isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> protocoloService.cambiarEstado(registrado.id(),
                new CambioEstadoRequest(ANDREA, EstadoProtocolo.OBSERVADO, " ")))
                .isInstanceOfSatisfying(RegistroIncompletoException.class,
                        ex -> assertThat(ex.getCampos()).containsKey("motivo"));

        ProtocoloDetalleResponse observado = protocoloService.cambiarEstado(registrado.id(),
                new CambioEstadoRequest(ANDREA, EstadoProtocolo.OBSERVADO, "Falta foto de placa"));
        assertThat(observado.estado()).isEqualTo("OBSERVADO");

        ProtocoloDetalleResponse corregido = protocoloService.actualizar(observado.id(),
                request(visita.id(), 1L, true, 18.5, List.of(aislacion(1L, Resultado.APROBADO))));
        assertThat(corregido.historial().get(corregido.historial().size() - 1).motivo())
                .isEqualTo("Corregido por el técnico");

        ProtocoloDetalleResponse formalizado = protocoloService.cambiarEstado(corregido.id(),
                new CambioEstadoRequest(RICARDO, EstadoProtocolo.FORMALIZADO, null));
        assertThat(formalizado.estado()).isEqualTo("FORMALIZADO");
        assertThatThrownBy(() -> protocoloService.cambiarEstado(formalizado.id(),
                new CambioEstadoRequest(RICARDO, EstadoProtocolo.ANULADO, "error")))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> protocoloService.cambiarEstado(formalizado.id(),
                new CambioEstadoRequest(RICARDO, EstadoProtocolo.BORRADOR, null)))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessage("Transición no permitida");
        assertThatThrownBy(() -> protocoloService.cambiarEstado(formalizado.id(),
                new CambioEstadoRequest(RICARDO, EstadoProtocolo.OBSERVADO, "x")))
                .isInstanceOf(SolicitudInvalidaException.class);

        VisitaResponse cerrada = visitaService.registrarRecepcion(visita.id(),
                new RecepcionRequest("Marcela Fuentes Rojas", "14.226.791-8", "Jefa de Mantenimiento"));
        assertThat(cerrada.cerrada()).isTrue();
        assertThat(cerrada.protocolosRegistrados()).isEqualTo(1);
        assertThatThrownBy(() -> visitaService.registrarRecepcion(visita.id(),
                new RecepcionRequest("Otra persona", "14.226.791-8", "Jefa")))
                .isInstanceOf(SolicitudInvalidaException.class);

        ActaResponse acta = visitaService.acta(visita.id(), ENVASADORA);
        assertThat(acta.recepcion().nombre()).isEqualTo("Marcela Fuentes Rojas");
        assertThat(acta.protocolos()).singleElement()
                .satisfies(p -> assertThat(p.equipoTag()).isEqualTo("TR-01"));
        assertThatThrownBy(() -> visitaService.acta(visita.id(), CLINICA)).isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void reglasAlCrearUnProtocolo() {
        VisitaResponse visita = visitaDeHoy();
        Long cerrada = visitaService.listar(RICARDO, false).get(0).id();

        assertThatThrownBy(() -> protocoloService.crear(new ProtocoloRequest(ENVASADORA, visita.id(), 1L, null, null,
                null, null, false, List.of()))).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> protocoloService.crear(request(cerrada, 1L, false, null, List.of())))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("cerrada");
        assertThatThrownBy(() -> protocoloService.crear(request(visita.id(), 5L, false, null, List.of())))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("no pertenece");
        assertThatThrownBy(() -> protocoloService.crear(request(visita.id(), 1L, false, null,
                List.of(new PruebaRequest(5L, "Relé", null, true, null, 1L, Resultado.APROBADO, null, false, null)))))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("no corresponde");
        assertThatThrownBy(() -> protocoloService.crear(request(visita.id(), 1L, false, null,
                List.of(new PruebaRequest(1L, "AT-T", null, true, null, 1L, Resultado.APROBADO, null, false,
                        List.of(new MedicionRequest(8L, 1.0)))))))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("no pertenece");
        assertThatThrownBy(() -> protocoloService.crear(request(visita.id(), 1L, false, null,
                List.of(new PruebaRequest(1L, "AT-T", null, true, null, 1L, Resultado.APROBADO, null, false,
                        List.of(new MedicionRequest(1L, 1.0), new MedicionRequest(1L, 2.0)))))))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("repetido");

        Long otraVisita = visitaService.listar(RICARDO, true).stream().filter(v -> v.numeroOt().equals("OT-2026-1187"))
                .findFirst().orElseThrow().id();
        when(usuarioClient.usuario(JAVIERA)).thenReturn(usuarios.get(JAVIERA));
        assertThatThrownBy(() -> protocoloService.crear(new ProtocoloRequest(JAVIERA, otraVisita, 2L, null, null,
                null, null, false, List.of()))).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> protocoloService.crear(request(otraVisita, 1L, false, null, List.of())))
                .isInstanceOf(ConflictoException.class);

        ProtocoloDetalleResponse creado = protocoloService.crear(request(visita.id(), 2L, false, null, List.of()));
        assertThatThrownBy(() -> protocoloService.actualizar(creado.id(), new ProtocoloRequest(JAVIERA, visita.id(),
                2L, null, null, null, null, false, List.of()))).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> protocoloService.actualizar("PT-2024-0312", request(visita.id(), 1L, false, null,
                List.of()))).isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("Borrador u Observado");
    }

    @Test
    void registrarExigeMotivosYMantenimientoSegunElTipoDeVisita() {
        Long visitaMantencion = visitaService.listar(MATIAS, true).stream()
                .filter(v -> v.numeroOt().equals("OT-2026-1216")).findFirst().orElseThrow().id();
        PruebaRequest noRealizada = new PruebaRequest(4L, "Polo 1", null, false, " ", null, null, null, false, null);

        assertThatThrownBy(() -> protocoloService.crear(new ProtocoloRequest(MATIAS, visitaMantencion, 5L, 20.0, null,
                null, null, true, List.of(noRealizada))))
                .isInstanceOfSatisfying(RegistroIncompletoException.class, ex -> assertThat(ex.getCampos())
                        .containsKeys("humedadPct", "descripcionMantenimiento", "pruebas",
                                "pruebas[0].motivoNoRealizada"));

        PruebaRequest sinMediciones = new PruebaRequest(4L, "Polo 1", null, true, null, 4L, Resultado.APROBADO, null,
                false, List.of());
        assertThatThrownBy(() -> protocoloService.crear(new ProtocoloRequest(MATIAS, visitaMantencion, 5L, 20.0, 30.0,
                "Ajuste", null, true, List.of(sinMediciones))))
                .isInstanceOfSatisfying(RegistroIncompletoException.class, ex -> assertThat(ex.getCampos())
                        .containsKeys("pruebas[0].instrumentoId", "pruebas[0].mediciones"));
    }

    @Test
    void historialMedicionesAnterioresEIndicadores() {
        List<ProtocoloResumenResponse> historial = protocoloService.historialEquipo(1L);
        assertThat(historial).extracting(ProtocoloResumenResponse::numero)
                .containsExactly("PT-2026-0401", "PT-2025-0288", "PT-2024-0312");
        assertThat(protocoloService.historialEquipo(99L)).isEmpty();

        List<MedicionAnteriorResponse> anteriores = protocoloService.medicionesAnteriores(1L,
                LocalDateTime.of(2026, 9, 26, 9, 0), null);
        assertThat(anteriores).extracting(MedicionAnteriorResponse::protocoloNumero).containsOnly("PT-2025-0288");
        assertThat(anteriores).extracting(MedicionAnteriorResponse::parametroCodigo)
                .containsExactly("LECTURA_1MIN", "LECTURA_10MIN", "TENSION_PRUEBA");
        assertThat(protocoloService.medicionesAnteriores(1L, null, "PT-2026-0401"))
                .extracting(MedicionAnteriorResponse::protocoloNumero).containsOnly("PT-2025-0288");

        assertThat(protocoloService.listar(EstadoProtocolo.FORMALIZADO, null, null, null)).hasSize(3);
        assertThat(protocoloService.listar(null, 1L, null, MATIAS)).hasSize(2);
        assertThat(protocoloService.listar(null, 77L, null, null)).isEmpty();
        assertThat(protocoloService.detalle("PT-2026-0389").historial()).hasSize(3);

        var indicadores = protocoloService.indicadores();
        assertThat(indicadores.total()).isEqualTo(6);
        assertThat(indicadores.porEstado()).containsEntry("FORMALIZADO", 3L).containsEntry("OBSERVADO", 1L);
        assertThat(indicadores.promedioDiasFormalizacion()).isEqualTo(2.1);
    }

    @Test
    void asignarVisitas() {
        LocalDate manana = LocalDate.now().plusDays(1);
        VisitaResponse nueva = visitaService.crear(new VisitaRequest(RICARDO, "ot-2026-1300", 4L, manana,
                TipoVisita.PRUEBAS_Y_MANT, ANDREA, List.of(MATIAS, JAVIERA, MATIAS)));
        assertThat(nueva.numeroOt()).isEqualTo("OT-2026-1300");
        assertThat(nueva.participantes()).hasSize(2);
        assertThat(nueva.clienteNombre()).isEqualTo("Clínica Los Aromos Ltda.");
        assertThat(visitaService.obtener(nueva.id()).tipoVisitaEtiqueta()).isEqualTo("Pruebas y mantenimiento");

        assertThatThrownBy(() -> visitaService.crear(new VisitaRequest(MATIAS, "OT-2026-1301", 4L, manana,
                TipoVisita.SOLO_PRUEBAS, ANDREA, List.of(MATIAS)))).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> visitaService.crear(new VisitaRequest(ANDREA, "OT-2026-1300", 4L, manana,
                TipoVisita.SOLO_PRUEBAS, ANDREA, List.of(MATIAS)))).isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> visitaService.crear(new VisitaRequest(ANDREA, "OT-2026-1302", 4L,
                LocalDate.of(2026, 1, 1), TipoVisita.SOLO_PRUEBAS, ANDREA, List.of(MATIAS))))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> visitaService.crear(new VisitaRequest(ANDREA, "OT-2026-1303", 4L, manana,
                TipoVisita.SOLO_PRUEBAS, MATIAS, List.of(MATIAS)))).isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> visitaService.crear(new VisitaRequest(ANDREA, "OT-2026-1304", 4L, manana,
                TipoVisita.SOLO_PRUEBAS, ANDREA, List.of(ENVASADORA)))).isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> visitaService.crear(new VisitaRequest(ANDREA, "OT-2026-1305", 4L, manana,
                TipoVisita.SOLO_PRUEBAS, ANDREA, List.of(3L)))).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> visitaService.obtener(999L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void editarVisitasNoCerradas() {
        LocalDate enUnaSemana = LocalDate.now().plusDays(7);
        VisitaResponse programada = visitaService.crear(new VisitaRequest(ANDREA, "OT-2026-1310", 4L, enUnaSemana,
                TipoVisita.SOLO_PRUEBAS, ANDREA, List.of(MATIAS)));

        VisitaResponse editada = visitaService.actualizar(programada.id(), new VisitaRequest(RICARDO, "ot-2026-1311",
                3L, enUnaSemana.plusDays(1), TipoVisita.PRUEBAS_Y_MANT, RICARDO, List.of(MATIAS, JAVIERA)));

        assertThat(editada.numeroOt()).isEqualTo("OT-2026-1311");
        assertThat(editada.instalacionId()).isEqualTo(3L);
        assertThat(editada.ingenieroCargoId()).isEqualTo(RICARDO);
        assertThat(editada.participantes()).hasSize(2);
        assertThat(visitaService.actualizar(programada.id(), new VisitaRequest(ANDREA, "OT-2026-1311", 3L,
                enUnaSemana, TipoVisita.SOLO_PRUEBAS, ANDREA, List.of(JAVIERA))).participantes()).hasSize(1);
        assertThatThrownBy(() -> visitaService.actualizar(programada.id(), new VisitaRequest(MATIAS, "OT-2026-1311", 3L,
                enUnaSemana, TipoVisita.SOLO_PRUEBAS, ANDREA, List.of(MATIAS)))).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> visitaService.actualizar(programada.id(), new VisitaRequest(ANDREA, "OT-2026-1187", 3L,
                enUnaSemana, TipoVisita.SOLO_PRUEBAS, ANDREA, List.of(MATIAS)))).isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> visitaService.actualizar(programada.id(), new VisitaRequest(ANDREA, "OT-2026-1311", 3L,
                LocalDate.now().minusDays(1), TipoVisita.SOLO_PRUEBAS, ANDREA, List.of(MATIAS))))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("anterior a hoy");

        VisitaResponse enCurso = visitaService.listar(RICARDO, true).stream()
                .filter(v -> v.numeroOt().equals("OT-2026-1187")).findFirst().orElseThrow();
        assertThatThrownBy(() -> visitaService.actualizar(enCurso.id(), new VisitaRequest(ANDREA, "OT-2026-1187", 3L,
                enCurso.fecha(), TipoVisita.SOLO_PRUEBAS, ANDREA, List.of(MATIAS))))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("instalación");
        assertThat(visitaService.actualizar(enCurso.id(), new VisitaRequest(ANDREA, "OT-2026-1187", 1L,
                enCurso.fecha(), TipoVisita.SOLO_PRUEBAS, ANDREA, List.of(MATIAS, JAVIERA))).participantes()).hasSize(2);

        VisitaResponse cerrada = visitaService.listar(RICARDO, false).stream()
                .filter(v -> v.numeroOt().equals("OT-2024-0951")).findFirst().orElseThrow();
        assertThatThrownBy(() -> visitaService.actualizar(cerrada.id(), new VisitaRequest(ANDREA, "OT-2024-0951", 1L,
                cerrada.fecha(), TipoVisita.SOLO_PRUEBAS, ANDREA, List.of(MATIAS))))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("cerrada");
    }

    @Test
    void eliminarSoloVisitasProgramadas() {
        VisitaResponse programada = visitaService.crear(new VisitaRequest(ANDREA, "OT-2026-1320", 4L,
                LocalDate.now().plusDays(3), TipoVisita.SOLO_PRUEBAS, ANDREA, List.of(MATIAS)));

        assertThatThrownBy(() -> visitaService.eliminar(programada.id(), MATIAS)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> visitaService.eliminar(visitaDeHoy().id(), ANDREA))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("programadas");
        visitaService.eliminar(programada.id(), RICARDO);

        assertThatThrownBy(() -> visitaService.obtener(programada.id())).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void cerrarUnaVisitaSinProtocolosNoEstaPermitido() {
        Long visitaMantencion = visitaService.listar(MATIAS, true).stream()
                .filter(v -> v.numeroOt().equals("OT-2026-1216")).findFirst().orElseThrow().id();

        assertThatThrownBy(() -> visitaService.registrarRecepcion(visitaMantencion,
                new RecepcionRequest("Patricio Navarro Soto", "13.349.876-1", "Superintendente")))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("no tiene protocolos");
    }

    @Test
    void anularUnProtocoloEnBorrador() {
        ProtocoloDetalleResponse creado = protocoloService.crear(request(visitaDeHoy().id(), 2L, false, null, List.of()));

        ProtocoloDetalleResponse anulado = protocoloService.cambiarEstado(creado.id(),
                new CambioEstadoRequest(ANDREA, EstadoProtocolo.ANULADO, "Equipo fuera de servicio"));

        assertThat(anulado.estado()).isEqualTo("ANULADO");
        assertThat(anulado.historial().get(1).motivo()).isEqualTo("Equipo fuera de servicio");
    }

    @Test
    void evidenciasDelProtocolo() {
        ProtocoloDetalleResponse creado = protocoloService.crear(request(visitaDeHoy().id(), 2L, false, null, List.of()));
        MockMultipartFile foto = new MockMultipartFile("archivo", "placa.jpg", "image/jpeg", new byte[] {1, 2, 3});

        EvidenciaResponse evidencia = evidenciaService.subir(creado.id(), MATIAS, foto, " Placa del equipo ");
        assertThat(evidencia.url()).isEqualTo("/evidencias/" + evidencia.id() + "/archivo");
        assertThat(evidencia.descripcion()).isEqualTo("Placa del equipo");
        assertThat(evidenciaService.listar(creado.id())).hasSize(1);
        assertThat(evidenciaService.archivo(evidencia.id()).contenido()).containsExactly(1, 2, 3);
        assertThat(protocoloService.detalle(creado.id()).evidencias()).hasSize(1);

        assertThatThrownBy(() -> evidenciaService.subir(creado.id(), MATIAS,
                new MockMultipartFile("archivo", "doc.pdf", "application/pdf", new byte[] {1}), null))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("JPG");
        assertThatThrownBy(() -> evidenciaService.subir(creado.id(), MATIAS,
                new MockMultipartFile("archivo", "vacia.png", "image/png", new byte[0]), null))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> evidenciaService.subir("PT-2024-0312", MATIAS, foto, null))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("Formalizado");
        assertThatThrownBy(() -> evidenciaService.eliminar(evidencia.id(), JAVIERA))
                .isInstanceOf(AccesoDenegadoException.class);

        evidenciaService.eliminar(evidencia.id(), RICARDO);
        assertThat(evidenciaService.listar(creado.id())).isEmpty();
        assertThatThrownBy(() -> evidenciaService.archivo(evidencia.id()))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> evidenciaService.listar("NO-EXISTE")).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
