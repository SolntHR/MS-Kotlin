package cl.induztek.usuarios

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class RegistroApiTest : ApiTestBase() {
    @Test fun `contrato exacto de Android crea pendiente y devuelve correo interno`() {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json(registro())))
            .andExpect(status().isCreated).andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.correoInterno").value("aperez.12345678@induztek.ejemplo"))
            .andExpect(jsonPath("$.estado").value("PENDIENTE_ASIGNACION_ROL"))
        val u = usuarios.findByCorreoInterno("aperez.12345678@induztek.ejemplo")!!
        assertNull(u.rolCodigo)
        assertNull(u.passwordHash)
        assertFalse(u.activo)
        assertEquals("123456785", u.rut)
        assertTrue(encoder.matches("luna", u.respuestaHash))
    }
    @Test fun `duplicados normalizados devuelven 409`() {
        service.registrar(registro())
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json(registro("otro@example.org", "123456785"))))
            .andExpect(status().isConflict).andExpect(jsonPath("$.codigo").value("USUARIO_DUPLICADO"))
    }
    @Test fun `json incompleto rol inyectado y rut invalido devuelven 400`() {
        val body = mapper.readTree(json(registro())) as com.fasterxml.jackson.databind.node.ObjectNode
        body.put("rolCodigo", "ADMIN")
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(body.toString())).andExpect(status().isBadRequest)
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isBadRequest)
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json(registro(rut = "123456789"))))
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.errores.rut").exists())
    }
    @Test fun `campos vacios correo invalido y excesos se rechazan`() {
        val body = mapper.readTree(json(registro())) as com.fasterxml.jackson.databind.node.ObjectNode
        body.put("nombre", "a".repeat(151)).put("correoPersonal", "incorrecto").put("respuestaSeguridad", " ")
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.errores.nombre").exists()).andExpect(jsonPath("$.errores.correoPersonal").exists())
    }
}
