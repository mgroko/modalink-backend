package org.mgroko.backend.notificaciones.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Envía los emails de notificación. En desarrollo apunta a Mailpit; en
 * producción puede apuntar a cualquier SMTP configurado por propiedades.
 */
@Component
@ConditionalOnProperty(
        name = "app.notificaciones.email.habilitado",
        havingValue = "true")
public class NotificacionEmailSender {

    private final JavaMailSender mailSender;
    private final String remitente;

    public NotificacionEmailSender(
            JavaMailSender mailSender,
            @Value("${app.notificaciones.email.remitente:no-responder@modalink.local}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    public void enviar(NotificacionEmailBuilder.EmailContent contenido) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(contenido.destinatario());
            helper.setSubject(contenido.asunto());
            helper.setText(contenido.texto(), contenido.html());
            mensaje.setHeader("X-Mailer", "ModaLink");
            mailSender.send(mensaje);
        } catch (MessagingException ex) {
            throw new MailPreparationException("No se pudo preparar el email de notificación.", ex);
        }
    }
}
