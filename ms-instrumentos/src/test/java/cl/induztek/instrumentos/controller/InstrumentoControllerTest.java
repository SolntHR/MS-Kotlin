package cl.induztek.instrumentos.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import cl.induztek.instrumentos.dto.CalibracionResponse;
import cl.induztek.instrumentos.dto.InstrumentoResponse;
import cl.induztek.instrumentos.dto.VigenciaResponse;
import cl.induztek.instrumentos.exception.ConflictoException;
import cl.induztek.instrumentos.exception.ManejadorErrores;
import cl.induztek.instrumentos.exception.RecursoNoEncontradoException;
import cl.induztek.instrumentos.exception.SolicitudInvalidaException;
import cl.induztek.instrumentos.model.EstadoCalibracion;
import cl.induztek.instrumentos.service.InstrumentoService;

class InstrumentoControllerTest {

    private static final String INSTRUMENTO =
            "{\"nombreTipo\":\"Megóhmetro 5 kV\",\"marca\":\"Megger\",\"modelo\":\"MIT525\",\"numeroSerie\":\"MIT-1\"}";
    private static final String CALIBRACION =
            "{\"fechaCalibracion\":\"2026-11-20\",\"fechaVencimiento\":\"2027-11-20\"}";

    private final InstrumentoService instrumentoService = mock(InstrumentoService.class);
    private MockMvc mockMvc;

    private final InstrumentoResponse megger = new InstrumentoResponse(1L, "Megóhmetro 5 kV", "Megger", "MIT525",
            "MIT-0045821", LocalDate.of(2026, 3, 15), LocalDate.of(2027, 3, 15), 112L, "VIGENTE", "Vigente");
    private final CalibracionResponse calibracion = new CalibracionResponse(6L, 1L, LocalDate.of(2026, 11, 20),
            LocalDate.of(2027, 11, 20), null);

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InstrumentoController(instrumentoService))
                .setControllerAdvice(new ManejadorErrores()).build();
    }

    @Test
    void listarYFiltrarPorEstado() throws Exception {
        when(instrumentoService.listar(null)).thenReturn(List.of(megger));
        when(instrumentoService.listar(EstadoCalibracion.VIGENTE)).thenReturn(List.of(megger));

        mockMvc.perform(get("/instrumentos")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fechaVencimiento").value("2027-03-15"));
        mockMvc.perform(get("/instrumentos").param("estado", "VIGENTE")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estadoEtiqueta").value("Vigente"));
        mockMvc.perform(get("/instrumentos").param("estado", "OTRO")).andExpect(status().isBadRequest());
    }

    @Test
    void obtenerCrearYActualizar() throws Exception {
        when(instrumentoService.obtener(1L)).thenReturn(megger);
        when(instrumentoService.obtener(8L)).thenThrow(new RecursoNoEncontradoException("No existe el instrumento 8"));
        when(instrumentoService.crear(any())).thenReturn(megger).thenThrow(new ConflictoException("Serie repetida"));
        when(instrumentoService.actualizar(eq(1L), any())).thenReturn(megger);

        mockMvc.perform(get("/instrumentos/1")).andExpect(status().isOk());
        mockMvc.perform(get("/instrumentos/8")).andExpect(status().isNotFound());
        mockMvc.perform(post("/instrumentos").contentType(MediaType.APPLICATION_JSON).content(INSTRUMENTO))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/instrumentos").contentType(MediaType.APPLICATION_JSON).content(INSTRUMENTO))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/instrumentos/1").contentType(MediaType.APPLICATION_JSON).content(INSTRUMENTO))
                .andExpect(status().isOk());
    }

    @Test
    void instrumentoSinDatosResponde400ConLosCampos() throws Exception {
        mockMvc.perform(post("/instrumentos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombreTipo").exists())
                .andExpect(jsonPath("$.campos.numeroSerie").exists());
        mockMvc.perform(post("/instrumentos").contentType(MediaType.APPLICATION_JSON).content("{x"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calibracionesYVigencia() throws Exception {
        when(instrumentoService.calibraciones(1L)).thenReturn(List.of(calibracion));
        when(instrumentoService.registrarCalibracion(eq(1L), any())).thenReturn(calibracion)
                .thenThrow(new SolicitudInvalidaException("La fecha de calibración no puede ser futura"));
        when(instrumentoService.vigencia(1L, LocalDate.of(2026, 9, 26)))
                .thenReturn(new VigenciaResponse(1L, LocalDate.of(2026, 9, 26), true, LocalDate.of(2027, 3, 15)));

        mockMvc.perform(get("/instrumentos/1/calibraciones")).andExpect(status().isOk());
        mockMvc.perform(post("/instrumentos/1/calibraciones").contentType(MediaType.APPLICATION_JSON).content(CALIBRACION))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/instrumentos/1/calibraciones").contentType(MediaType.APPLICATION_JSON).content(CALIBRACION))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La fecha de calibración no puede ser futura"));
        mockMvc.perform(get("/instrumentos/1/vigencia").param("fecha", "2026-09-26")).andExpect(status().isOk())
                .andExpect(jsonPath("$.vigente").value(true));
    }

    @Test
    void listaVaciaResponde204() throws Exception {
        when(instrumentoService.listar(EstadoCalibracion.SIN_CALIBRACION)).thenReturn(List.of());
        when(instrumentoService.calibraciones(5L)).thenReturn(List.of());

        mockMvc.perform(get("/instrumentos").param("estado", "SIN_CALIBRACION")).andExpect(status().isNoContent());
        mockMvc.perform(get("/instrumentos/5/calibraciones")).andExpect(status().isNoContent());
    }

    @Test
    void errorInesperadoResponde500() throws Exception {
        when(instrumentoService.obtener(3L)).thenThrow(new IllegalStateException("fallo"));

        mockMvc.perform(get("/instrumentos/3")).andExpect(status().isInternalServerError());
    }
}
