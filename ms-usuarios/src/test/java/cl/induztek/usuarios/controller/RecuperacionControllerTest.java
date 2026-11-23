package cl.induztek.usuarios.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import cl.induztek.usuarios.exception.ManejadorErrores;
import cl.induztek.usuarios.exception.SolicitudInvalidaException;
import cl.induztek.usuarios.service.RecuperacionService;

class RecuperacionControllerTest {

    private static final String CORREO = "mgodoy@induztek.ejemplo";
    private static final String PREGUNTA = "¿Cuál es el nombre de tu primera mascota?";

    private final RecuperacionService recuperacionService = mock(RecuperacionService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RecuperacionController(recuperacionService))
                .setControllerAdvice(new ManejadorErrores()).build();
    }

    @Test
    void verificarYRestablecerConPregunta() throws Exception {
        mockMvc.perform(post("/usuarios/recuperacion/pregunta/verificar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + CORREO + "\",\"pregunta\":\"" + PREGUNTA + "\",\"respuesta\":\"firulais\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/usuarios/recuperacion/pregunta").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + CORREO + "\",\"pregunta\":\"" + PREGUNTA + "\",\"respuesta\":\"firulais\","
                        + "\"nuevaContrasena\":\"ebf37cea11e6aaeaa21dcc80cfc2e18ae66653e9f1100bcd9e8a7af933236d6b\"}"))
                .andExpect(status().isNoContent());
        verify(recuperacionService).restablecerConPregunta(CORREO, PREGUNTA, "firulais", "ebf37cea11e6aaeaa21dcc80cfc2e18ae66653e9f1100bcd9e8a7af933236d6b");
    }

    @Test
    void respuestaIncorrectaResponde400() throws Exception {
        doThrow(new SolicitudInvalidaException("Los datos no coinciden")).when(recuperacionService)
                .verificarPregunta(CORREO, PREGUNTA, "rocky");

        mockMvc.perform(post("/usuarios/recuperacion/pregunta/verificar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + CORREO + "\",\"pregunta\":\"" + PREGUNTA + "\",\"respuesta\":\"rocky\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Los datos no coinciden"));
    }

    @Test
    void solicitarCodigoResponde202SinRevelarSiElCorreoExiste() throws Exception {
        mockMvc.perform(post("/usuarios/recuperacion/codigo").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + CORREO + "\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensaje").exists());
        verify(recuperacionService).solicitarCodigo(CORREO);
    }

    @Test
    void verificarYRestablecerConCodigo() throws Exception {
        mockMvc.perform(post("/usuarios/recuperacion/codigo/verificar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + CORREO + "\",\"codigo\":\"482913\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/usuarios/recuperacion/codigo/restablecer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + CORREO + "\",\"codigo\":\"482913\",\"nuevaContrasena\":\"ebf37cea11e6aaeaa21dcc80cfc2e18ae66653e9f1100bcd9e8a7af933236d6b\"}"))
                .andExpect(status().isNoContent());
        verify(recuperacionService).restablecerConCodigo(CORREO, "482913", "ebf37cea11e6aaeaa21dcc80cfc2e18ae66653e9f1100bcd9e8a7af933236d6b");
    }

    @Test
    void codigoConFormatoInvalidoResponde400() throws Exception {
        mockMvc.perform(post("/usuarios/recuperacion/codigo/verificar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + CORREO + "\",\"codigo\":\"12ab\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.codigo").exists());
    }
}
