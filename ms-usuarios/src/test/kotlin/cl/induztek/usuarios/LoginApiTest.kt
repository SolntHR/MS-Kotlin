package cl.induztek.usuarios

import cl.induztek.usuarios.api.LoginRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Duration

class LoginApiTest : ApiTestBase() {
    @Test fun `login entrega bearer y perfil sin secretos`() {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(LoginRequest(admin.correoInterno.uppercase(), clave))))
            .andExpect(status().isOk).andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(43200)).andExpect(jsonPath("$.usuario.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.usuario.respuestaHash").doesNotExist())
    }
    @Test fun `clave incorrecta usuario inexistente y pendiente devuelven 401`() {
        val pendiente = cuenta(null, activo = false, password = false)
        for (correo in listOf(admin.correoInterno, "inexistente@example.org", pendiente.correoInterno)) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(LoginRequest(correo, "incorrecta"))))
                .andExpect(status().isUnauthorized).andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"))
        }
    }
    @Test fun `cuenta desactivada no inicia sesion`() {
        val u = cuenta(activo = false)
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(LoginRequest(u.correoInterno, clave))))
            .andExpect(status().isForbidden).andExpect(jsonPath("$.codigo").value("CUENTA_NO_HABILITADA"))
    }
    @Test fun `token expira y logout revoca inmediatamente`() {
        val token = token()
        assertNotNull(sesiones.findById(secrets.tokenHash(token)).orElse(null))
        mvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer $token")).andExpect(status().isNoContent)
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer $token")).andExpect(status().isUnauthorized)
        val otro = token()
        clock.advance(Duration.ofHours(12))
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer $otro")).andExpect(status().isUnauthorized)
    }
    @Test fun `limita ataques de contrasena`() {
        repeat(10) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(LoginRequest(admin.correoInterno, "incorrecta")))).andExpect(status().isUnauthorized)
        }
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(LoginRequest(admin.correoInterno, clave)))).andExpect(status().isTooManyRequests)
    }
}
