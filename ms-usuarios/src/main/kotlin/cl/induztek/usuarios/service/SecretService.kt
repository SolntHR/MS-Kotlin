package cl.induztek.usuarios.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class SecretService(@Value("\${app.code-secret}") private val secret: String) {
    init { require(secret.toByteArray(UTF_8).size >= 32) { "RECOVERY_CODE_SECRET debe tener al menos 32 bytes" } }
    private val random = SecureRandom()
    fun token(): String = ByteArray(32).also(random::nextBytes).let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
    fun tokenHash(token: String): String = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(UTF_8)).toHex()
    fun codigo(): String = random.nextInt(100_000_000).toString().padStart(8, '0')
    fun codigoHash(usuarioId: Long, codigo: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(UTF_8), "HmacSHA256"))
        return mac.doFinal("$usuarioId:$codigo".toByteArray(UTF_8)).toHex()
    }
    fun coincide(a: String, b: String) = MessageDigest.isEqual(a.toByteArray(UTF_8), b.toByteArray(UTF_8))
    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
}
