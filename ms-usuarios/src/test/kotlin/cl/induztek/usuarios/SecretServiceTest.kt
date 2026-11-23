package cl.induztek.usuarios

import cl.induztek.usuarios.service.SecretService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder

class SecretServiceTest {
    private val s = SecretService("test-secret-with-at-least-32-bytes-123")
    @Test fun `tokens aleatorios de 256 bits y hash persistible`() {
        val a = s.token()
        assertTrue(a.matches(Regex("[A-Za-z0-9_-]{43}")))
        assertNotEquals(a, s.token())
        assertEquals(64, s.tokenHash(a).length)
        assertNotEquals(a, s.tokenHash(a))
    }
    @Test fun `codigo ligado al usuario y al secreto`() {
        assertTrue(s.codigo().matches(Regex("[0-9]{8}")))
        assertTrue(s.coincide(s.codigoHash(1, "12345678"), s.codigoHash(1, "12345678")))
        assertFalse(s.coincide(s.codigoHash(1, "12345678"), s.codigoHash(2, "12345678")))
        assertFalse(s.coincide(s.codigoHash(1, "12345678"), s.codigoHash(1, "87654321")))
    }
    @Test fun `exige secreto largo`() { assertThrows(IllegalArgumentException::class.java) { SecretService("corto") } }
    @Test fun `hashes de la misma clave tienen sales diferentes`() {
        val encoder = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()
        val a = encoder.encode("Clave segura 123")
        val b = encoder.encode("Clave segura 123")
        assertNotEquals(a, b)
        assertTrue(encoder.matches("Clave segura 123", a))
        assertFalse(encoder.matches("otra", a))
    }
}
