package cl.induztek.usuarios.service

import cl.induztek.usuarios.api.ApiException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
class RecoveryMailer(private val sender: JavaMailSender, @Value("\${app.mail-from}") private val from: String) {
    fun enviar(destino: String, codigo: String) {
        val mensaje = SimpleMailMessage().apply {
            setFrom(from)
            setTo(destino)
            subject = "Código de acceso Induztek"
            text = "Tu código para establecer tu contraseña es: $codigo\nCaduca en 10 minutos y se puede usar una sola vez.\nSi no lo solicitaste, ignora este correo."
        }
        try { sender.send(mensaje) }
        catch (_: MailException) { throw ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CORREO_NO_DISPONIBLE", "No se pudo enviar el correo. Inténtalo más tarde") }
    }
}
