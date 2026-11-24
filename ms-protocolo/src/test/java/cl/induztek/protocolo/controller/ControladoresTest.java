package cl.induztek.protocolo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import cl.induztek.protocolo.dto.ActaResponse;
import cl.induztek.protocolo.dto.ArchivoEvidencia;
import cl.induztek.protocolo.dto.EvidenciaResponse;
import cl.induztek.protocolo.dto.IndicadoresResponse;
import cl.induztek.protocolo.dto.MedicionAnteriorResponse;
import cl.induztek.protocolo.dto.ProtocoloDetalleResponse;
import cl.induztek.protocolo.dto.ProtocoloResumenResponse;
import cl.induztek.protocolo.dto.TipoPruebaResponse;
import cl.induztek.protocolo.dto.VisitaResponse;
import cl.induztek.protocolo.dto.remoto.EquipoRemoto;
import cl.induztek.protocolo.exception.AccesoDenegadoException;
import cl.induztek.protocolo.exception.ManejadorErrores;
import cl.induztek.protocolo.exception.RegistroIncompletoException;
import cl.induztek.protocolo.exception.ServicioNoDisponibleException;
import cl.induztek.protocolo.model.EstadoProtocolo;
import cl.induztek.protocolo.service.CatalogoService;
import cl.induztek.protocolo.service.EvidenciaService;
import cl.induztek.protocolo.service.ProtocoloService;
import cl.induztek.protocolo.service.VisitaService;

class ControladoresTest {

    private final CatalogoService catalogoService = mock(CatalogoService.class);
    private final VisitaService visitaService = mock(VisitaService.class);
    private final ProtocoloService protocoloService = mock(ProtocoloService.class);
    private final EvidenciaService evidenciaService = mock(EvidenciaService.class);
    private MockMvc mockMvc;

    private final VisitaResponse visita = new VisitaResponse(6L, "OT-2026-1215", 1L, "Planta Lampa", 8L,
            "Envasadora del Pacífico S.A.", LocalDate.of(2026, 11, 23), "SOLO_PRUEBAS", "Solo pruebas", 5L,
            "Andrea Contreras Silva", List.of(), false, null, 0, 0);
    private final ProtocoloDetalleResponse detalle = new ProtocoloDetalleResponse("abc", "PT-2026-ABC", 6L,
            "OT-2026-1215", "SOLO_PRUEBAS", 1L, "TR-01", "Transformador", "Planta Lampa", "Envasadora", 1L, "Matías",
            LocalDateTime.of(2026, 11, 23, 10, 0), 18.0, 55.0, null, null, "BORRADOR", "Borrador", null, List.of(),
            List.of(), List.of());
    private final EvidenciaResponse evidencia = new EvidenciaResponse(1L, "placa.jpg", "image/jpeg", 3, null, 1L,
            LocalDateTime.of(2026, 11, 23, 10, 5), "/evidencias/1/archivo");

    private static final String PROTOCOLO = "{\"usuarioId\":1,\"visitaId\":6,\"equipoId\":1,\"registrar\":false,"
            + "\"pruebas\":[{\"tipoPruebaId\":1,\"elemento\":\"AT-T\",\"realizada\":true,"
            + "\"mediciones\":[{\"parametroId\":1,\"valor\":11000}]}]}";
    private static final String VISITA = "{\"usuarioId\":7,\"numeroOt\":\"OT-2026-1300\",\"instalacionId\":1,"
            + "\"fecha\":\"2026-11-24\",\"tipoVisita\":\"SOLO_PRUEBAS\",\"ingenieroCargoId\":5,\"participantes\":[1,2]}";

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CatalogoController(catalogoService),
                new VisitaController(visitaService), new ProtocoloController(protocoloService),
                new EvidenciaController(evidenciaService)).setControllerAdvice(new ManejadorErrores()).build();
    }

    @Test
    void tiposDePrueba() throws Exception {
        when(catalogoService.tiposPrueba(1L)).thenReturn(List.of(new TipoPruebaResponse(1L, "AISLACION",
                "Resistencia de aislación", List.of())));

        mockMvc.perform(get("/tipos-prueba").param("tipoEquipoId", "1")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("AISLACION"));
    }

    @Test
    void visitas() throws Exception {
        when(visitaService.listar(1L, true)).thenReturn(List.of(visita));
        when(visitaService.obtener(6L)).thenReturn(visita);
        when(visitaService.crear(any())).thenReturn(visita);
        when(visitaService.equiposDisponibles(6L, null)).thenReturn(List.of(new EquipoRemoto(1L, 1L, "Planta Lampa",
                8L, "TR-01", 1L, "Transformador", null, null, null, null, null, null)));
        when(visitaService.registrarRecepcion(eq(6L), any())).thenReturn(visita);
        when(visitaService.acta(6L, 8L)).thenReturn(new ActaResponse(visita, null, List.of()));
        when(visitaService.acta(6L, 11L)).thenThrow(new AccesoDenegadoException("Esta acta no está disponible"));

        mockMvc.perform(get("/visitas").param("usuarioId", "1").param("abiertas", "true")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fecha").value("2026-11-23"));
        mockMvc.perform(get("/visitas/6")).andExpect(status().isOk());
        mockMvc.perform(post("/visitas").contentType(MediaType.APPLICATION_JSON).content(VISITA))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/visitas/6/equipos-disponibles")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tag").value("TR-01"));
        mockMvc.perform(post("/visitas/6/recepcion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"receptorNombre\":\"Marcela Fuentes\",\"receptorRut\":\"14.226.791-8\",\"receptorCargo\":\"Jefa\"}"))
                .andExpect(status().isOk());
        when(visitaService.actualizar(eq(6L), any())).thenReturn(visita);
        mockMvc.perform(put("/visitas/6").contentType(MediaType.APPLICATION_JSON).content(VISITA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.numeroOt").value("OT-2026-1215"));
        mockMvc.perform(delete("/visitas/6").param("usuarioId", "5")).andExpect(status().isNoContent());
        verify(visitaService).eliminar(6L, 5L);
        mockMvc.perform(get("/visitas/6/acta").param("usuarioId", "8")).andExpect(status().isOk());
        mockMvc.perform(get("/visitas/6/acta").param("usuarioId", "11")).andExpect(status().isForbidden());
    }

    @Test
    void validacionesDeVisitaYRecepcion() throws Exception {
        mockMvc.perform(post("/visitas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"numeroOt\":\"1300\",\"participantes\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.numeroOt").value("La OT debe tener el formato OT-2026-0001"))
                .andExpect(jsonPath("$.campos.participantes").exists());
        mockMvc.perform(post("/visitas/6/recepcion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"receptorNombre\":\"\",\"receptorRut\":\"123\",\"receptorCargo\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.receptorRut").exists());
        mockMvc.perform(get("/visitas")).andExpect(status().isBadRequest());
    }

    @Test
    void protocolos() throws Exception {
        when(protocoloService.listar(EstadoProtocolo.BORRADOR, null, 6L, null)).thenReturn(List.of(
                new ProtocoloResumenResponse("abc", "PT-2026-ABC", 6L, "OT-2026-1215", 1L, "TR-01", "Transformador",
                        "Planta Lampa", "Envasadora", 1L, "Matías Godoy Herrera", LocalDateTime.of(2026, 11, 23, 10, 0),
                        "BORRADOR", "Borrador", null, null, null, null)));
        when(protocoloService.detalle("abc")).thenReturn(detalle);
        when(protocoloService.crear(any())).thenReturn(detalle);
        when(protocoloService.actualizar(eq("abc"), any())).thenReturn(detalle);
        when(protocoloService.cambiarEstado(eq("abc"), any())).thenReturn(detalle);
        when(protocoloService.historialEquipo(1L)).thenReturn(List.of());
        when(protocoloService.medicionesAnteriores(eq(1L), any(LocalDateTime.class), isNull())).thenReturn(List.of(
                new MedicionAnteriorResponse(1L, "AT-T", "PT-2025-0288", LocalDateTime.of(2025, 9, 13, 9, 30),
                        "LECTURA_1MIN", 9800.0, "MΩ")));
        when(protocoloService.indicadores()).thenReturn(new IndicadoresResponse(6, Map.of("FORMALIZADO", 3L), 2.1));

        mockMvc.perform(get("/protocolos").param("estado", "BORRADOR").param("visitaId", "6"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].numero").value("PT-2026-ABC"));
        mockMvc.perform(get("/protocolos/abc")).andExpect(status().isOk()).andExpect(jsonPath("$.equipoTag").value("TR-01"));
        mockMvc.perform(post("/protocolos").contentType(MediaType.APPLICATION_JSON).content(PROTOCOLO))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/protocolos/abc").contentType(MediaType.APPLICATION_JSON).content(PROTOCOLO))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/protocolos/abc/estado").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioId\":5,\"estado\":\"FORMALIZADO\"}")).andExpect(status().isOk());
        mockMvc.perform(get("/protocolos/equipo/1/historial")).andExpect(status().isNoContent());
        mockMvc.perform(get("/protocolos/equipo/1/mediciones-anteriores").param("antesDe", "2026-09-26T09:00:00"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].valor").value(9800.0));
        mockMvc.perform(get("/protocolos/indicadores")).andExpect(status().isOk())
                .andExpect(jsonPath("$.promedioDiasFormalizacion").value(2.1));
    }

    @Test
    void registroIncompletoDevuelveLosCamposParaMarcarlosEnRojo() throws Exception {
        when(protocoloService.crear(any())).thenThrow(new RegistroIncompletoException(Map.of(
                "temperaturaC", "Ingresa la temperatura ambiente",
                "pruebas[0].instrumentoId", "Selecciona el instrumento")));

        mockMvc.perform(post("/protocolos").contentType(MediaType.APPLICATION_JSON).content(PROTOCOLO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.temperaturaC").value("Ingresa la temperatura ambiente"))
                .andExpect(jsonPath("$.campos['pruebas[0].instrumentoId']").value("Selecciona el instrumento"));
    }

    @Test
    void validacionesDelCuerpoDelProtocolo() throws Exception {
        mockMvc.perform(post("/protocolos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioId\":1,\"visitaId\":6,\"equipoId\":1,\"humedadPct\":120,"
                        + "\"pruebas\":[{\"tipoPruebaId\":1,\"elemento\":\"\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.humedadPct").exists())
                .andExpect(jsonPath("$.campos['pruebas[0].elemento']").exists());
        mockMvc.perform(patch("/protocolos/abc/estado").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioId\":5,\"estado\":\"INVENTADO\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void servicioRemotoCaidoResponde503() throws Exception {
        when(protocoloService.detalle("xyz")).thenThrow(new ServicioNoDisponibleException("El servicio de usuarios no está disponible"));

        mockMvc.perform(get("/protocolos/xyz")).andExpect(status().isServiceUnavailable());
    }

    @Test
    void evidencias() throws Exception {
        when(evidenciaService.subir(eq("abc"), eq(1L), any(), eq("Placa"))).thenReturn(evidencia);
        when(evidenciaService.listar("abc")).thenReturn(List.of(evidencia));
        when(evidenciaService.archivo(1L)).thenReturn(new ArchivoEvidencia("placa.jpg", "image/jpeg", new byte[] {1, 2, 3}));

        mockMvc.perform(multipart("/protocolos/abc/evidencias")
                .file(new MockMultipartFile("archivo", "placa.jpg", "image/jpeg", new byte[] {1, 2, 3}))
                .param("usuarioId", "1").param("descripcion", "Placa"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.url").value("/evidencias/1/archivo"));
        mockMvc.perform(get("/protocolos/abc/evidencias")).andExpect(status().isOk());
        mockMvc.perform(get("/evidencias/1/archivo")).andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"placa.jpg\""))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
        mockMvc.perform(delete("/evidencias/1").param("usuarioId", "1")).andExpect(status().isNoContent());
        verify(evidenciaService).eliminar(1L, 1L);
        mockMvc.perform(multipart("/protocolos/abc/evidencias").param("usuarioId", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listasVaciasResponden204() throws Exception {
        when(visitaService.listar(9L, null)).thenReturn(List.of());
        when(protocoloService.listar(EstadoProtocolo.ANULADO, null, null, null)).thenReturn(List.of());
        when(evidenciaService.listar("abc")).thenReturn(List.of());

        mockMvc.perform(get("/visitas").param("usuarioId", "9")).andExpect(status().isNoContent());
        mockMvc.perform(get("/protocolos").param("estado", "ANULADO")).andExpect(status().isNoContent());
        mockMvc.perform(get("/protocolos/abc/evidencias")).andExpect(status().isNoContent());
    }

    @Test
    void errorInesperadoResponde500() throws Exception {
        when(protocoloService.detalle(anyString())).thenThrow(new IllegalStateException("fallo"));

        mockMvc.perform(get("/protocolos/otro")).andExpect(status().isInternalServerError());
    }
}
