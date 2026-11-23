package cl.induztek.usuarios.service

import cl.induztek.usuarios.api.ApiException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant

// Límite adicional por identidad. Los intentos de código también se persisten en la BD.
// Para varias réplicas, sustituir este límite local por Redis o un límite en el ingress.
@Component
class RateLimiter(private val clock: Clock, private val secrets: SecretService) {
    private data class Ventana(var intentos: Int, val hasta: Instant)
    private val ventanas = mutableMapOf<String, Ventana>()
    @Synchronized
    fun verificar(ambito: String, identidad: String, maximo: Int, duracion: Duration) {
        val ahora = clock.instant()
        ventanas.entries.removeIf { it.value.hasta <= ahora }
        val clave = secrets.tokenHash("$ambito:$identidad")
        val ventana = ventanas[clave] ?: run {
            if (ventanas.size >= 10_000) rechazar()
            Ventana(0, ahora.plus(duracion)).also { ventanas[clave] = it }
        }
        if (ventana.intentos >= maximo) rechazar()
        ventana.intentos++
    }
    private fun rechazar(): Nothing = throw ApiException(HttpStatus.TOO_MANY_REQUESTS, "DEMASIADOS_INTENTOS", "Demasiados intentos; espera antes de volver a intentar")
}
