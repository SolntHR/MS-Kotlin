package cl.induztek.usuarios

import cl.induztek.usuarios.api.ApiException
import cl.induztek.usuarios.service.RateLimiter
import cl.induztek.usuarios.service.SecretService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration

class RateLimiterTest {
    @Test fun `limita identidad ambito y libera ventana expirada`() {
        val clock = MutableClock()
        val limiter = RateLimiter(clock, SecretService("test-secret-at-least-32-bytes-long"))
        limiter.verificar("login", "a", 1, Duration.ofMinutes(15))
        val e = assertThrows(ApiException::class.java) { limiter.verificar("login", "a", 1, Duration.ofMinutes(15)) }
        assertEquals(429, e.status.value())
        limiter.verificar("login", "b", 1, Duration.ofMinutes(15))
        limiter.verificar("recuperar", "a", 1, Duration.ofMinutes(15))
        clock.advance(Duration.ofMinutes(15))
        limiter.verificar("login", "a", 1, Duration.ofMinutes(15))
    }
}
