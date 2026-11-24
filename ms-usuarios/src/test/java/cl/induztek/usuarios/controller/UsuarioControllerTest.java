package cl.induztek.usuarios.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import cl.induztek.usuarios.dto.LoginRequest;
import cl.induztek.usuarios.dto.RegistroResponse;
import cl.induztek.usuarios.dto.UsuarioResponse;
import cl.induztek.usuarios.exception.AccesoDenegadoException;
import cl.induztek.usuarios.exception.ConflictoException;
import cl.induztek.usuarios.exception.CredencialesInvalidasException;
import cl.induztek.usuarios.exception.DemasiadosIntentosException;
import cl.induztek.usuarios.exception.ManejadorErrores;
import cl.induztek.usuarios.exception.RecursoNoEncontradoException;
import cl.induztek.usuarios.model.Rol;
import cl.induztek.usuarios.service.UsuarioService;

class UsuarioControllerTest {

    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private MockMvc mockMvc;

    private final UsuarioResponse matias = new UsuarioResponse(1L, "TECNICO", "Técnico de terreno", "17.284.519-3",
            "Matías Godoy Herrera", "mgodoy@induztek.ejemplo", "matias.godoy@gmail.com", "+56 9 8123 4567", true, true,
            null, null, null);

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UsuarioController(usuarioService))
                .setControllerAdvice(new ManejadorErrores()).build();
    }

    @Test
    void loginCorrectoResponde200ConElUsuario() throws Exception {
        when(usuarioService.login(any(LoginRequest.class))).thenReturn(matias);

        mockMvc.perform(post("/usuarios/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"mgodoy@induztek.ejemplo\",\"contrasena\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rolCodigo").value("TECNICO"))
                .andExpect(jsonPath("$.nombre").value("Matías Godoy Herrera"));
    }

    @Test
    void loginIncorrectoResponde401() throws Exception {
        when(usuarioService.login(any(LoginRequest.class)))
                .thenThrow(new CredencialesInvalidasException("Correo o contraseña incorrectos"));

        mockMvc.perform(post("/usuarios/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"mgodoy@induztek.ejemplo\",\"contrasena\":\"999999\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"))
                .andExpect(jsonPath("$.ruta").value("/usuarios/login"));
    }

    @Test
    void loginDeCuentaDesactivadaResponde403() throws Exception {
        when(usuarioService.login(any(LoginRequest.class))).thenThrow(new AccesoDenegadoException("Cuenta desactivada"));

        mockMvc.perform(post("/usuarios/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"mgodoy@induztek.ejemplo\",\"contrasena\":\"123456\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registroConDatosInvalidosResponde400ConLosCampos() throws Exception {
        mockMvc.perform(post("/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"\",\"rut\":\"17.284.519-4\",\"correoPersonal\":\"no-es-correo\","
                        + "\"telefono\":\"123\",\"preguntaSeguridad\":\"\",\"respuestaSeguridad\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.rut").exists())
                .andExpect(jsonPath("$.campos.correoPersonal").exists())
                .andExpect(jsonPath("$.campos.telefono").exists());
    }

    @Test
    void registroValidoResponde201() throws Exception {
        when(usuarioService.registrar(any())).thenReturn(new RegistroResponse(20L, "atorres@induztek.ejemplo",
                "PENDIENTE_ASIGNACION_ROL", "Tu cuenta quedó pendiente"));

        mockMvc.perform(post("/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana Torres\",\"rut\":\"15906334-8\",\"correoPersonal\":\"ana@gmail.com\","
                        + "\"telefono\":\"+56 9 1234 5678\","
                        + "\"preguntaSeguridad\":\"¿Cuál es tu comida favorita?\",\"respuestaSeguridad\":\"pizza\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correoInterno").value("atorres@induztek.ejemplo"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE_ASIGNACION_ROL"));
    }

    @Test
    void jsonMalFormadoResponde400() throws Exception {
        mockMvc.perform(post("/usuarios/login").contentType(MediaType.APPLICATION_JSON).content("{correo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El formato de la solicitud no es válido"));
    }

    @Test
    void listarPreguntasYUsuarios() throws Exception {
        when(usuarioService.preguntasSeguridad()).thenReturn(List.of("¿Cuál es tu comida favorita?"));
        when(usuarioService.listar(Rol.TECNICO, true, null)).thenReturn(List.of(matias));

        mockMvc.perform(get("/usuarios/preguntas")).andExpect(status().isOk()).andExpect(jsonPath("$[0]").exists());
        mockMvc.perform(get("/usuarios").param("rol", "TECNICO").param("activo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void listaVaciaResponde204() throws Exception {
        when(usuarioService.listar(null, false, null)).thenReturn(List.of());

        mockMvc.perform(get("/usuarios").param("activo", "false")).andExpect(status().isNoContent());
    }

    @Test
    void rolInexistenteEnElFiltroResponde400() throws Exception {
        mockMvc.perform(get("/usuarios").param("rol", "ADMIN")).andExpect(status().isBadRequest());
    }

    @Test
    void obtenerUsuarioExistenteEInexistente() throws Exception {
        when(usuarioService.obtener(1L)).thenReturn(matias);
        when(usuarioService.obtener(99L)).thenThrow(new RecursoNoEncontradoException("Usuario no encontrado"));

        mockMvc.perform(get("/usuarios/1")).andExpect(status().isOk());
        mockMvc.perform(get("/usuarios/99")).andExpect(status().isNotFound());
    }

    @Test
    void crearUsuarioResponde201YCorreoRepetido409() throws Exception {
        String cuerpo = "{\"rol\":\"INGENIERO\",\"nombre\":\"Pedro Rojas\",\"rut\":\"15906334-8\","
                + "\"correoPersonal\":\"pedro.rojas@gmail.com\",\"telefono\":\"+56 9 1111 2222\","
                + "\"preguntaSeguridad\":\"¿Cuál es tu comida favorita?\",\"respuestaSeguridad\":\"porotos\"}";
        when(usuarioService.crear(any())).thenReturn(matias)
                .thenThrow(new ConflictoException("Ya existe una cuenta con ese correo personal"));

        mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correoPersonal").value("matias.godoy@gmail.com"))
                .andExpect(jsonPath("$.tieneContrasena").value(true));
        mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict());
    }

    @Test
    void actualizarCambiarRolEstadoYContrasena() throws Exception {
        when(usuarioService.actualizar(eq(1L), any())).thenReturn(matias);
        when(usuarioService.cambiarRol(1L, Rol.INGENIERO, 7L, "hash-jefe")).thenReturn(matias);
        when(usuarioService.cambiarActivo(1L, false)).thenReturn(matias);

        mockMvc.perform(put("/usuarios/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Matías Godoy\",\"rut\":\"17284519-3\",\"telefono\":\"+56 9 8123 4567\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/usuarios/1/rol").contentType(MediaType.APPLICATION_JSON)
                .content("{\"rol\":\"INGENIERO\",\"jefeId\":7,\"contrasena\":\"hash-jefe\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/usuarios/1/rol").contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"INGENIERO\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/usuarios/1/activo").contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/usuarios/1/contrasena").contentType(MediaType.APPLICATION_JSON)
                .content("{\"actual\":\"123456\",\"nueva\":\"ebf37cea11e6aaeaa21dcc80cfc2e18ae66653e9f1100bcd9e8a7af933236d6b\"}"))
                .andExpect(status().isNoContent());
        verify(usuarioService).cambiarContrasena(1L, "123456", "ebf37cea11e6aaeaa21dcc80cfc2e18ae66653e9f1100bcd9e8a7af933236d6b");
    }

    @Test
    void demasiadosIntentosResponde429() throws Exception {
        when(usuarioService.login(any(LoginRequest.class)))
                .thenThrow(new DemasiadosIntentosException("Demasiados intentos. Espera 15 minutos."));

        mockMvc.perform(post("/usuarios/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"mgodoy@induztek.ejemplo\",\"contrasena\":\"123456\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.estado").value(429));
    }

    @Test
    void errorInesperadoResponde500() throws Exception {
        when(usuarioService.obtener(5L)).thenThrow(new IllegalStateException("fallo"));

        mockMvc.perform(get("/usuarios/5")).andExpect(status().isInternalServerError());
    }
}
