package cl.induztek.instalaciones.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import cl.induztek.instalaciones.dto.ComunaResponse;
import cl.induztek.instalaciones.dto.EquipoResponse;
import cl.induztek.instalaciones.dto.InstalacionResponse;
import cl.induztek.instalaciones.dto.RegionResponse;
import cl.induztek.instalaciones.dto.TipoEquipoResponse;
import cl.induztek.instalaciones.exception.ConflictoException;
import cl.induztek.instalaciones.exception.ManejadorErrores;
import cl.induztek.instalaciones.exception.RecursoNoEncontradoException;
import cl.induztek.instalaciones.exception.ServicioNoDisponibleException;
import cl.induztek.instalaciones.service.CatalogoService;
import cl.induztek.instalaciones.service.EquipoService;
import cl.induztek.instalaciones.service.InstalacionService;

class ControladoresTest {

    private final CatalogoService catalogoService = mock(CatalogoService.class);
    private final InstalacionService instalacionService = mock(InstalacionService.class);
    private final EquipoService equipoService = mock(EquipoService.class);
    private MockMvc mockMvc;

    private final InstalacionResponse planta = new InstalacionResponse(1L, 8L, "Envasadora del Pacífico S.A.",
            "Planta Lampa", "Sector industrial", 1L, "Lampa", "Región Metropolitana", "Inducción obligatoria");
    private final EquipoResponse tr01 = new EquipoResponse(1L, 1L, "Planta Lampa", 8L, "TR-01", 1L, "Transformador",
            "PRIMARIO", "Rhona", "RH-18733", "RH-18733", null, null, 1500.0, 12.0, 72.0, null);

    private static final String INSTALACION = "{\"clienteId\":8,\"nombre\":\"Bodega\",\"comunaId\":1}";
    private static final String EQUIPO = "{\"instalacionId\":1,\"tag\":\"TR-03\",\"tipoEquipoId\":1}";

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CatalogoController(catalogoService),
                new InstalacionController(instalacionService, equipoService), new EquipoController(equipoService))
                .setControllerAdvice(new ManejadorErrores()).build();
    }

    @Test
    void catalogos() throws Exception {
        when(catalogoService.regiones()).thenReturn(List.of(new RegionResponse(1L, "Región Metropolitana")));
        when(catalogoService.comunas(1L)).thenReturn(List.of(new ComunaResponse(1L, "Lampa", 1L, "RM")));
        when(catalogoService.comunas(null)).thenReturn(List.of(new ComunaResponse(1L, "Lampa", 1L, "RM")));
        when(catalogoService.tiposEquipo()).thenReturn(List.of(new TipoEquipoResponse(1L, "Transformador", "PRIMARIO")));
        when(catalogoService.tipoEquipo(1L)).thenReturn(new TipoEquipoResponse(1L, "Transformador", "PRIMARIO"));

        mockMvc.perform(get("/regiones")).andExpect(status().isOk()).andExpect(jsonPath("$[0].nombre").value("Región Metropolitana"));
        mockMvc.perform(get("/regiones/1/comunas")).andExpect(status().isOk()).andExpect(jsonPath("$[0].nombre").value("Lampa"));
        mockMvc.perform(get("/comunas")).andExpect(status().isOk());
        mockMvc.perform(get("/tipos-equipo")).andExpect(status().isOk()).andExpect(jsonPath("$[0].clasificacion").value("PRIMARIO"));
        mockMvc.perform(get("/tipos-equipo/1")).andExpect(status().isOk());
    }

    @Test
    void consultarInstalaciones() throws Exception {
        when(instalacionService.listar(8L)).thenReturn(List.of(planta));
        when(instalacionService.obtener(1L)).thenReturn(planta);
        when(instalacionService.obtener(9L)).thenThrow(new RecursoNoEncontradoException("No existe la instalación 9"));
        when(equipoService.listar(1L, null, null)).thenReturn(List.of(tr01));

        mockMvc.perform(get("/instalaciones").param("clienteId", "8")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clienteNombre").value("Envasadora del Pacífico S.A."));
        mockMvc.perform(get("/instalaciones/1")).andExpect(status().isOk());
        mockMvc.perform(get("/instalaciones/1/equipos")).andExpect(status().isOk()).andExpect(jsonPath("$[0].tag").value("TR-01"));
        mockMvc.perform(get("/instalaciones/9")).andExpect(status().isNotFound());
    }

    @Test
    void crearYActualizarInstalacion() throws Exception {
        when(instalacionService.crear(any())).thenReturn(planta)
                .thenThrow(new ServicioNoDisponibleException("El servicio de usuarios no está disponible"));
        when(instalacionService.actualizar(eq(1L), any())).thenReturn(planta);

        mockMvc.perform(post("/instalaciones").contentType(MediaType.APPLICATION_JSON).content(INSTALACION))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/instalaciones").contentType(MediaType.APPLICATION_JSON).content(INSTALACION))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(put("/instalaciones/1").contentType(MediaType.APPLICATION_JSON).content(INSTALACION))
                .andExpect(status().isOk());
    }

    @Test
    void instalacionSinDatosObligatoriosResponde400() throws Exception {
        mockMvc.perform(post("/instalaciones").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.clienteId").exists())
                .andExpect(jsonPath("$.campos.nombre").exists())
                .andExpect(jsonPath("$.campos.comunaId").exists());
    }

    @Test
    void equipos() throws Exception {
        when(equipoService.listar(null, 8L, null)).thenReturn(List.of(tr01));
        when(equipoService.obtener(1L)).thenReturn(tr01);
        when(equipoService.crear(any())).thenReturn(tr01).thenThrow(new ConflictoException("Ya existe el TAG TR-03"));
        when(equipoService.actualizar(eq(1L), any())).thenReturn(tr01);

        mockMvc.perform(get("/equipos").param("clienteId", "8")).andExpect(status().isOk());
        mockMvc.perform(get("/equipos/1")).andExpect(status().isOk()).andExpect(jsonPath("$.potenciaKva").value(1500.0));
        mockMvc.perform(post("/equipos").contentType(MediaType.APPLICATION_JSON).content(EQUIPO)).andExpect(status().isCreated());
        mockMvc.perform(post("/equipos").contentType(MediaType.APPLICATION_JSON).content(EQUIPO)).andExpect(status().isConflict());
        mockMvc.perform(put("/equipos/1").contentType(MediaType.APPLICATION_JSON).content(EQUIPO)).andExpect(status().isOk());
    }

    @Test
    void equipoConTagInvalidoOJsonMalFormado() throws Exception {
        mockMvc.perform(post("/equipos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"instalacionId\":1,\"tag\":\"TR 01!\",\"tipoEquipoId\":1,\"anio\":1800}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.tag").exists())
                .andExpect(jsonPath("$.campos.anio").exists());
        mockMvc.perform(post("/equipos").contentType(MediaType.APPLICATION_JSON).content("{tag"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/equipos/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void listasVaciasResponden204() throws Exception {
        when(instalacionService.listar(10L)).thenReturn(List.of());
        when(equipoService.listar(null, 10L, null)).thenReturn(List.of());

        mockMvc.perform(get("/instalaciones").param("clienteId", "10")).andExpect(status().isNoContent());
        mockMvc.perform(get("/equipos").param("clienteId", "10")).andExpect(status().isNoContent());
    }

    @Test
    void errorInesperadoResponde500() throws Exception {
        when(equipoService.obtener(2L)).thenThrow(new IllegalStateException("fallo"));

        mockMvc.perform(get("/equipos/2")).andExpect(status().isInternalServerError());
    }
}
