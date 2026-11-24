package cl.induztek.usuarios.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CorreoService {

    private final ObjectProvider<JavaMailSender> proveedorCorreo;
    private final String remitente;

    public CorreoService(ObjectProvider<JavaMailSender> proveedorCorreo,
            @Value("${app.correo.remitente:no-responder@induztek.cl}") String remitente) {
        this.proveedorCorreo = proveedorCorreo;
        this.remitente = remitente;
    }

    public boolean enviarCodigoRecuperacion(String destino, String nombre, String codigo) {
        String texto = "Hola " + nombre + ",\n\n"
                + "Tu código para restablecer la contraseña de Induztek Protocolos es: " + codigo + "\n\n"
                + "El código vence en 10 minutos. Si no lo pediste, ignora este correo.";
        boolean enviado = enviar(destino, "Código para restablecer tu contraseña", texto);
        if (!enviado) {
            log.warn("Correo de recuperación no enviado a {}.", destino);
        }
        return enviado;
    }

    public boolean enviarCodigoActivacion(String destino, String nombre, String correoAcceso, String codigo) {
        String texto = "Hola " + nombre + ",\n\n"
                + "Tu cuenta de Induztek Protocolos ya tiene un perfil asignado.\n"
                + "Tu correo de acceso es: " + correoAcceso + "\n"
                + "Tu código para crear tu contraseña es: " + codigo + "\n\n"
                + "En la app elige \"Recuperar contraseña\" > \"Por correo\" e ingresa este código. Vence en 24 horas.";
        boolean enviado = enviar(destino, "Activa tu cuenta de Induztek Protocolos", texto);
        if (!enviado) {
            log.warn("Correo de activación no enviado a {}.", destino);
        }
        return enviado;
    }

    private boolean enviar(String destino, String asunto, String texto) {
        JavaMailSender enviador = proveedorCorreo.getIfAvailable();
        if (enviador == null) {
            log.warn("No hay servidor de correo configurado");
            return false;
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(destino);
            mensaje.setSubject(asunto);
            mensaje.setText(texto);
            enviador.send(mensaje);
            return true;
        } catch (MailException e) {
            log.warn("No se pudo enviar el correo a {}: {}", destino, e.getMessage());
            return false;
        }
    }
}
