package cl.induztek.usuarios

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class UsuariosConsultaTest : ApiTestBase() {
    @Test fun `lista paginada y ficha solo incluyen datos publicos`() {
        val t = token()
        mvc.perform(get("/api/v1/usuarios?pagina=0&tamano=1").header("Authorization", "Bearer $t"))
            .andExpect(status().isOk).andExpect(jsonPath("$.totalElementos").value(1)).andExpect(jsonPath("$.contenido[0].rolCodigo").value("ADMIN"))
            .andExpect(jsonPath("$.contenido[0].respuestaHash").doesNotExist())
        mvc.perform(get("/api/v1/usuarios/${admin.id}").header("Authorization", "Bearer $t")).andExpect(status().isOk)
    }
    @Test fun `paginacion e IDs invalidos son controlados`() {
        val t = token()
        for (query in listOf("pagina=-1", "tamano=0", "tamano=101", "pagina=x")) {
            mvc.perform(get("/api/v1/usuarios?$query").header("Authorization", "Bearer $t")).andExpect(status().isBadRequest)
        }
        mvc.perform(get("/api/v1/usuarios/99999").header("Authorization", "Bearer $t")).andExpect(status().isNotFound)
    }
}
