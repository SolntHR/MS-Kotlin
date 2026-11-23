package cl.induztek.usuarios

import cl.induztek.usuarios.api.ApiException
import cl.induztek.usuarios.service.RecoveryMailer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.springframework.mail.MailSendException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender

class RecoveryMailerTest {
    @Test fun `envia codigo solo al destinatario indicado`() {
        val sender = mock(JavaMailSender::class.java)
        RecoveryMailer(sender, "no-reply@induztek.cl").enviar("personal@example.org", "12345678")
        val captor = ArgumentCaptor.forClass(SimpleMailMessage::class.java)
        verify(sender).send(captor.capture())
        assertArrayEquals(arrayOf("personal@example.org"), captor.value.to)
        assertTrue(captor.value.text!!.contains("12345678"))
    }
    @Test fun `traduce fallo smtp sin revelar detalles`() {
        val sender = mock(JavaMailSender::class.java)
        doThrow(MailSendException("credencial sensible")).`when`(sender).send(any(SimpleMailMessage::class.java))
        val e = assertThrows(ApiException::class.java) { RecoveryMailer(sender, "a@b.cl").enviar("c@b.cl", "12345678") }
        assertEquals(503, e.status.value())
        assertFalse(e.message.contains("sensible"))
    }
}
