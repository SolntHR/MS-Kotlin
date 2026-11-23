package cl.induztek.usuarios

import cl.induztek.usuarios.domain.Rol
import cl.induztek.usuarios.api.ApiException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class RolesApiTest : ApiTestBase() {
    @Test fun `admin asigna rol y habilita pendiente sin exponer secretos`() {
        val creado = service.registrar(registro())
        mvc.perform(patch("/api/v1/usuarios/${creado.id}/rol").header("Authorization", "Bearer ${token()}")
            .contentType(MediaType.APPLICATION_JSON).content("""{"rolCodigo":"INGENIERO"}"""))
            .andExpect(status().isOk).andExpect(jsonPath("$.rolCodigo").value("INGENIERO")).andExpect(jsonPath("$.activo").value(true))
            .andExpect(jsonPath("$.passwordHash").doesNotExist()).andExpect(jsonPath("$.respuestaSeguridad").doesNotExist())
    }
    @Test fun `personal de terreno no puede asignarse privilegios`() {
        for (rol in Rol.entries.filter { it != Rol.ADMIN }) {
            val u = cuenta(rol)
            mvc.perform(patch("/api/v1/usuarios/${u.id}/rol").header("Authorization", "Bearer ${token(u)}")
                .contentType(MediaType.APPLICATION_JSON).content("""{"rolCodigo":"ADMIN"}"""))
                .andExpect(status().isForbidden)
            sesiones.deleteAllInBatch()
            usuarios.deleteById(u.id)
        }
    }
    @Test fun `ultimo administrador no puede desactivarse ni perder rol`() {
        val token = token()
        mvc.perform(patch("/api/v1/usuarios/${admin.id}/activo").header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON).content("""{"activo":false}"""))
            .andExpect(status().isConflict).andExpect(jsonPath("$.codigo").value("ULTIMO_ADMINISTRADOR"))
        mvc.perform(patch("/api/v1/usuarios/${admin.id}/rol").header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON).content("""{"rolCodigo":"CLIENTE"}"""))
            .andExpect(status().isConflict)
    }
    @Test fun `desactivar revoca tokens y activar requiere rol`() {
        val pendiente = service.registrar(registro())
        val adminToken = token()
        mvc.perform(patch("/api/v1/usuarios/${pendiente.id}/activo").header("Authorization", "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON).content("""{"activo":true}"""))
            .andExpect(status().isConflict).andExpect(jsonPath("$.codigo").value("ROL_REQUERIDO"))
        service.asignarRol(admin.id, pendiente.id, Rol.CLIENTE)
        val u = cuenta(rut = "222222222")
        val previo = token(u)
        mvc.perform(patch("/api/v1/usuarios/${u.id}/activo").header("Authorization", "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON).content("""{"activo":false}"""))
            .andExpect(status().isOk).andExpect(jsonPath("$.activo").value(false))
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer $previo")).andExpect(status().isUnauthorized)
    }
    @Test fun `cambio de rol revoca tokens anteriores`() {
        val u = cuenta()
        val previo = token(u)
        service.asignarRol(admin.id, u.id, Rol.JEFE_TECNICO)
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer $previo")).andExpect(status().isUnauthorized)
    }
    @Test fun `administrador adicional puede ser desactivado y degradado`() {
        val otro = cuenta(Rol.ADMIN)
        assertFalse(service.cambiarActivo(admin.id, otro.id, false).activo)
        assertEquals(Rol.CLIENTE, service.asignarRol(admin.id, otro.id, Rol.CLIENTE).rolCodigo)
        val error = assertThrows(ApiException::class.java) { service.asignarRol(otro.id, admin.id, Rol.CLIENTE) }
        assertEquals(403, error.status.value())
        assertEquals(Rol.ADMIN, usuarios.findById(admin.id).orElseThrow().rolCodigo)
    }
    @Test fun `roles desconocidos campos nulos e IDs inexistentes tienen errores precisos`() {
        val t = token()
        mvc.perform(patch("/api/v1/usuarios/${admin.id}/rol").header("Authorization", "Bearer $t")
            .contentType(MediaType.APPLICATION_JSON).content("""{"rolCodigo":"ROOT"}""" )).andExpect(status().isBadRequest)
        mvc.perform(patch("/api/v1/usuarios/${admin.id}/rol").header("Authorization", "Bearer $t")
            .contentType(MediaType.APPLICATION_JSON).content("""{"rolCodigo":0}""" )).andExpect(status().isBadRequest)
        mvc.perform(patch("/api/v1/usuarios/${admin.id}/activo").header("Authorization", "Bearer $t")
            .contentType(MediaType.APPLICATION_JSON).content("""{"activo":null}""" )).andExpect(status().isBadRequest)
        mvc.perform(patch("/api/v1/usuarios/${admin.id}/activo").header("Authorization", "Bearer $t")
            .contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isBadRequest)
        mvc.perform(patch("/api/v1/usuarios/999999/rol").header("Authorization", "Bearer $t")
            .contentType(MediaType.APPLICATION_JSON).content("""{"rolCodigo":"CLIENTE"}""" )).andExpect(status().isNotFound)
    }
}
