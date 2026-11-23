package cl.induztek.usuarios

import cl.induztek.usuarios.config.AdminBootstrap
import cl.induztek.usuarios.domain.Rol
import cl.induztek.usuarios.service.RegistroValidator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.boot.DefaultApplicationArguments

class AdminBootstrapTest : ApiTestBase() {
    private fun bootstrap(email: String, password: String = clave, answer: String = "luna") =
        AdminBootstrap(usuarios, encoder, RegistroValidator(), clock, email, password, answer)
    @Test fun `base vacia permite crear primer administrador`() {
        usuarios.deleteAllInBatch()
        bootstrap("admin@example.org").run(DefaultApplicationArguments())
        val creado = usuarios.findByCorreoInterno("admin@example.org")!!
        assertEquals(Rol.ADMIN, creado.rolCodigo)
        assertTrue(creado.activo)
        assertTrue(encoder.matches(clave, creado.passwordHash))
        assertTrue(encoder.matches("luna", creado.respuestaHash))
    }
    @Test fun `reiniciar no cambia contrasena de administrador existente`() {
        bootstrap(admin.correoInterno, "otra clave segura").run(DefaultApplicationArguments())
        assertTrue(encoder.matches(clave, usuarios.findById(admin.id).orElseThrow().passwordHash))
    }
    @Test fun `bootstrap no eleva usuarios existentes ni crea administradores adicionales`() {
        val u = cuenta()
        assertThrows(IllegalArgumentException::class.java) { bootstrap(u.correoInterno).run(DefaultApplicationArguments()) }
        assertThrows(IllegalArgumentException::class.java) { bootstrap("nuevo@example.org").run(DefaultApplicationArguments()) }
    }
    @Test fun `rechaza configuracion incompleta claves cortas y correo invalido`() {
        for (b in listOf(bootstrap(""), bootstrap("sin-arroba"), bootstrap("nuevo@example.org", "corta"), bootstrap("nuevo@example.org", answer = ""))) {
            assertThrows(IllegalArgumentException::class.java) { b.run(DefaultApplicationArguments()) }
        }
        bootstrap("", "", "").run(DefaultApplicationArguments())
    }
}
