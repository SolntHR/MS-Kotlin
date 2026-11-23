package cl.induztek.usuarios

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class SecurityApiTest : ApiTestBase() {
    @Test fun `sin token se rechaza informacion privada`() {
        mvc.perform(get("/api/v1/usuarios")).andExpect(status().isUnauthorized).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        mvc.perform(get("/api/v1/usuarios/me")).andExpect(status().isUnauthorized)
    }
    @Test fun `tokens falsos mal formados y basic se rechazan`() {
        for (header in listOf("Basic abc", "Bearer corto", "Bearer " + "a".repeat(43))) {
            mvc.perform(get("/api/v1/usuarios/me").header("Authorization", header)).andExpect(status().isUnauthorized).andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"))
        }
    }
    @Test fun `perfil propio funciona pero otros usuarios requieren administrador`() {
        val u = cuenta()
        val t = token(u)
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer $t")).andExpect(status().isOk).andExpect(jsonPath("$.id").value(u.id))
        mvc.perform(get("/api/v1/usuarios/${admin.id}").header("Authorization", "Bearer $t")).andExpect(status().isForbidden)
    }
    @Test fun `swagger y health son publicos`() {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk)
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk).andExpect(jsonPath("$.paths['/api/v1/auth/registro'].post.responses['201']").exists())
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk)
    }
    @Test fun `metodos json y media type tienen respuestas consistentes`() {
        val t = token()
        mvc.perform(put("/api/v1/usuarios/${admin.id}").header("Authorization", "Bearer $t")).andExpect(status().isMethodNotAllowed)
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{mal" )).andExpect(status().isBadRequest)
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.TEXT_PLAIN).content("texto")).andExpect(status().isUnsupportedMediaType)
    }
}
