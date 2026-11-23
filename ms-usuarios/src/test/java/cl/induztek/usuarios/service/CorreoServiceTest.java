package cl.induztek.usuarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class CorreoServiceTest {

    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> proveedor = mock(ObjectProvider.class);
    private final JavaMailSender enviador = mock(JavaMailSender.class);
    private final CorreoService correoService = new CorreoService(proveedor, "no-responder@induztek.cl");

    @Test
    void enviaElCodigoAlCorreoDelUsuario() {
        when(proveedor.getIfAvailable()).thenReturn(enviador);

        boolean enviado = correoService.enviarCodigoRecuperacion("mgodoy@induztek.ejemplo", "Matías", "123456");

        ArgumentCaptor<SimpleMailMessage> mensaje = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(enviador).send(mensaje.capture());
        assertThat(enviado).isTrue();
        assertThat(mensaje.getValue().getTo()).containsExactly("mgodoy@induztek.ejemplo");
        assertThat(mensaje.getValue().getText()).contains("123456");
    }

    @Test
    void enviaElCodigoDeActivacionConElCorreoDeAcceso() {
        when(proveedor.getIfAvailable()).thenReturn(enviador);

        assertThat(correoService.enviarCodigoActivacion("ana@gmail.com", "Ana", "atorres@induztek.ejemplo", "654321"))
                .isTrue();

        ArgumentCaptor<SimpleMailMessage> mensaje = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(enviador).send(mensaje.capture());
        assertThat(mensaje.getValue().getTo()).containsExactly("ana@gmail.com");
        assertThat(mensaje.getValue().getText()).contains("atorres@induztek.ejemplo").contains("654321");
    }

    @Test
    void devuelveFalseSiNoHayServidorDeCorreo() {
        when(proveedor.getIfAvailable()).thenReturn(null);

        assertThat(correoService.enviarCodigoRecuperacion("a@b.cl", "Ana", "000111")).isFalse();
    }

    @Test
    void devuelveFalseSiElEnvioFalla() {
        when(proveedor.getIfAvailable()).thenReturn(enviador);
        doThrow(new MailSendException("sin conexión")).when(enviador).send(any(SimpleMailMessage.class));

        assertThat(correoService.enviarCodigoActivacion("a@b.cl", "Ana", "ana@induztek.ejemplo", "x")).isFalse();
    }
}
