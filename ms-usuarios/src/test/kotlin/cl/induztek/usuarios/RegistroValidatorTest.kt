package cl.induztek.usuarios

import cl.induztek.usuarios.api.ApiException
import cl.induztek.usuarios.api.RegistroRequest
import cl.induztek.usuarios.service.RegistroValidator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RegistroValidatorTest {
    private val v = RegistroValidator()
    @Test fun `valida digito verificador y rechaza formato incompleto`() {
        assertTrue(v.rutValido("12.345.678-5"))
        assertFalse(v.rutValido("12.345.678-9"))
        assertFalse(v.rutValido("000000000"))
        assertFalse(v.rutValido("12K"))
    }
    @Test fun `normaliza los formatos enviados por Android`() {
        assertEquals("123456785", v.rut("12.345.678-5"))
        assertEquals("+56 9 1234 5678", v.telefono("912345678"))
        assertEquals("+56 9 1234 5678", v.telefono("+56 (9) 1234-5678"))
        assertEquals("", v.telefono("812345678"))
        assertEquals("Ana Pérez", v.nombre(" Ana   Pérez "))
        assertEquals("luna azul", v.respuesta(" LUNA   Azul "))
        assertEquals("ana@example.org", v.correo(" ANA@example.org "))
    }
    @Test fun `genera correo sin tildes con identificador unico`() {
        assertEquals("aperez.12345678@induztek.ejemplo", v.correoInterno("Ána Pérez Soto", "123456785", "induztek.ejemplo"))
    }
    @Test fun `devuelve errores por campo`() {
        val error = assertThrows(ApiException::class.java) { v.validar(RegistroRequest("1", "x", "a", "a@b.cl", "otra", " ")) }
        assertEquals(setOf("nombre", "rut", "telefono", "preguntaSeguridad", "respuestaSeguridad"), error.errores.keys)
    }
    @Test fun `acepta las cinco preguntas de Android`() {
        v.preguntas.forEach { v.validar(RegistroRequest("Ana Pérez", "123456785", "912345678", "a@b.cl", it, "luna")) }
    }
}
