package cl.induztek.usuarios

import cl.induztek.usuarios.api.*
import cl.induztek.usuarios.domain.Usuario
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.springframework.http.MediaType
import org.springframework.mail.MailSendException
import org.springframework.mail.SimpleMailMessage
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RecuperacionApiTest : ApiTestBase() {
    private fun solicitud(u: Usuario) = RecuperacionRequest(u.correoInterno, pregunta, " LUNA ")
    private fun enviar(u: Usuario): String {
        mvc.perform(post("/api/v1/auth/recuperacion/solicitar").contentType(MediaType.APPLICATION_JSON).content(json(solicitud(u)))).andExpect(status().isAccepted)
        val captor = ArgumentCaptor.forClass(SimpleMailMessage::class.java)
        verify(sender, atLeastOnce()).send(captor.capture())
        assertArrayEquals(arrayOf(u.correoPersonal), captor.value.to)
        return Regex("[0-9]{8}").find(captor.value.text!!)!!.value
    }
    private fun restablecer(u: Usuario, codigo: String, nueva: String = "Nueva clave segura 2026", confirmacion: String = nueva) =
        mvc.perform(post("/api/v1/auth/recuperacion/restablecer").contentType(MediaType.APPLICATION_JSON).content(json(RestablecerRequest(u.correoInterno, codigo, nueva, confirmacion))))

    @Test fun `codigo de correo permite primera clave despues de asignar rol`() {
        val registro = service.registrar(registro())
        service.asignarRol(admin.id, registro.id, cl.induztek.usuarios.domain.Rol.TECNICO)
        val u = usuarios.findById(registro.id).orElseThrow()
        val codigo = enviar(u)
        val guardado = codigos.findById(u.id).orElseThrow()
        assertNotEquals(codigo, guardado.codigoHash)
        restablecer(u, codigo).andExpect(status().isNoContent)
        val actualizado = usuarios.findById(u.id).orElseThrow()
        assertTrue(encoder.matches("Nueva clave segura 2026", actualizado.passwordHash))
        assertTrue(codigos.findById(u.id).isEmpty)
        restablecer(u, codigo).andExpect(status().isBadRequest)
        assertNotNull(auth.login(LoginRequest(u.correoInterno, "Nueva clave segura 2026")).accessToken)
    }
    @Test fun `restablecer revoca todas las sesiones anteriores`() {
        val u = cuenta()
        val previo = token(u)
        val codigo = enviar(u)
        restablecer(u, codigo).andExpect(status().isNoContent)
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer $previo")).andExpect(status().isUnauthorized)
    }
    @Test fun `dos solicitudes simultaneas no pueden consumir el mismo codigo`() {
        val u = cuenta()
        val codigo = enviar(u)
        val inicio = CountDownLatch(1)
        Executors.newFixedThreadPool(2).use { pool ->
            val resultados = (1..2).map {
                pool.submit(Callable {
                    check(inicio.await(10, TimeUnit.SECONDS))
                    try {
                        auth.restablecer(RestablecerRequest(u.correoInterno, codigo, "Nueva clave segura 2026", "Nueva clave segura 2026"))
                        204
                    } catch (e: ApiException) { e.status.value() }
                })
            }
            inicio.countDown()
            assertEquals(listOf(204, 400), resultados.map { it.get(15, TimeUnit.SECONDS) }.sorted())
        }
        assertTrue(codigos.findById(u.id).isEmpty)
    }
    @Test fun `cinco codigos incorrectos agotan incluso el codigo correcto`() {
        val u = cuenta()
        val codigo = enviar(u)
        val incorrecto = if (codigo == "00000000") "11111111" else "00000000"
        repeat(5) { restablecer(u, incorrecto).andExpect(status().isBadRequest).andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO")) }
        assertEquals(5, codigos.findById(u.id).orElseThrow().intentos)
        restablecer(u, codigo).andExpect(status().isBadRequest)
        assertTrue(encoder.matches(clave, usuarios.findById(u.id).orElseThrow().passwordHash))
    }
    @Test fun `codigo caduca exactamente a los diez minutos`() {
        val u = cuenta()
        val codigo = enviar(u)
        clock.advance(Duration.ofMinutes(10))
        restablecer(u, codigo).andExpect(status().isBadRequest)
    }
    @Test fun `reenviar espera sesenta segundos e invalida el codigo anterior`() {
        val u = cuenta()
        val primero = enviar(u)
        auth.solicitarCodigo(solicitud(u))
        verify(sender, times(1)).send(any(SimpleMailMessage::class.java))
        clock.advance(Duration.ofSeconds(60))
        val segundo = enviar(u)
        assertNotEquals(primero, segundo)
        restablecer(u, primero).andExpect(status().isBadRequest)
        restablecer(u, segundo).andExpect(status().isNoContent)
    }
    @Test fun `cuentas inexistentes respuestas erradas inactivas y pendientes no envian correo`() {
        val pendiente = cuenta(null, activo = false, password = false)
        val inactivo = cuenta(rut = "222222222", activo = false)
        val casos = listOf(
            RecuperacionRequest("inexistente@example.org", pregunta, "luna"),
            RecuperacionRequest(admin.correoInterno, pregunta, "respuesta incorrecta"),
            RecuperacionRequest(admin.correoInterno, "otra pregunta", "luna"),
            solicitud(pendiente), solicitud(inactivo)
        )
        val respuestas = casos.map {
            mvc.perform(post("/api/v1/auth/recuperacion/solicitar").contentType(MediaType.APPLICATION_JSON).content(json(it)))
                .andExpect(status().isAccepted).andReturn().response.contentAsString
        }
        assertEquals(1, respuestas.distinct().size)
        verifyNoInteractions(sender)
    }
    @Test fun `smtp fallido devuelve 503 y revierte el codigo`() {
        val u = cuenta()
        doThrow(MailSendException("SMTP privado")).`when`(sender).send(any(SimpleMailMessage::class.java))
        mvc.perform(post("/api/v1/auth/recuperacion/solicitar").contentType(MediaType.APPLICATION_JSON).content(json(solicitud(u))))
            .andExpect(status().isServiceUnavailable).andExpect(jsonPath("$.codigo").value("CORREO_NO_DISPONIBLE"))
        assertTrue(codigos.findById(u.id).isEmpty)
    }
    @Test fun `confirmacion distinta clave corta formato y cuenta ausente no cambian clave`() {
        val u = cuenta()
        val codigo = enviar(u)
        restablecer(u, codigo, confirmacion = "otra clave segura").andExpect(status().isBadRequest).andExpect(jsonPath("$.errores.confirmacion").exists())
        restablecer(u, codigo, nueva = "corta").andExpect(status().isBadRequest)
        restablecer(u, "abcdef").andExpect(status().isBadRequest)
        restablecer(admin, codigo).andExpect(status().isBadRequest)
        assertTrue(encoder.matches(clave, usuarios.findById(u.id).orElseThrow().passwordHash))
    }
}
