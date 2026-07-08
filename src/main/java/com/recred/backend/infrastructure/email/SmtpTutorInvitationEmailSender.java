package com.recred.backend.infrastructure.email;

import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.domain.services.TutorInvitationEmailSender;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class SmtpTutorInvitationEmailSender implements TutorInvitationEmailSender {

    private static final DateTimeFormatter EXPIRATION_FORMAT =
            DateTimeFormatter.ofPattern("d/M/yyyy HH:mm");

    private final SmtpMailProperties properties;

    public SmtpTutorInvitationEmailSender(SmtpMailProperties properties) {
        this.properties = properties;
    }

    @Override
    public void sendInvitation(TutorInvitation invitation, String invitationLink) {
        try {
            MimeMessage message = new MimeMessage(session());
            message.setFrom(new InternetAddress(fromAddress()));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(invitation.getEmail()));
            message.setSubject("Invitacion para unirte a Recred", "UTF-8");
            message.setContent(content(invitation, invitationLink));
            Transport.send(message);
        } catch (MessagingException exception) {
            throw new IllegalStateException(
                    "No se pudo enviar el email de invitacion al tutor", exception);
        }
    }

    private Session session() {
        Properties smtpProperties = new Properties();
        smtpProperties.put("mail.smtp.host", properties.host());
        smtpProperties.put("mail.smtp.port", properties.port());
        smtpProperties.put("mail.smtp.auth", String.valueOf(properties.auth()));
        smtpProperties.put("mail.smtp.starttls.enable", String.valueOf(properties.startTls()));
        if (properties.sslTrust() != null && !properties.sslTrust().isBlank()) {
            smtpProperties.put("mail.smtp.ssl.trust", properties.sslTrust());
        }
        smtpProperties.put("mail.smtp.connectiontimeout",
                String.valueOf(properties.connectionTimeout()));
        smtpProperties.put("mail.smtp.timeout", String.valueOf(properties.timeout()));
        smtpProperties.put("mail.smtp.writetimeout", String.valueOf(properties.writeTimeout()));

        return Session.getInstance(smtpProperties, authenticator());
    }

    private Authenticator authenticator() {
        if (!properties.auth()) {
            return null;
        }
        return new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(properties.username(), properties.password());
            }
        };
    }

    private String fromAddress() {
        return properties.from() == null || properties.from().isBlank()
                ? properties.username()
                : properties.from();
    }

    private Multipart content(TutorInvitation invitation, String invitationLink)
            throws MessagingException {
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(textBody(invitation, invitationLink), "UTF-8");

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody(invitation, invitationLink), "text/html; charset=UTF-8");

        Multipart multipart = new MimeMultipart("alternative");
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(htmlPart);
        return multipart;
    }

    private String textBody(TutorInvitation invitation, String invitationLink) {
        String school = invitation.getSchool() == null
                ? "Tu colegio"
                : invitation.getSchool().getNombre();
        String greeting = invitation.getFirstName() == null || invitation.getFirstName().isBlank()
                ? "Hola"
                : "Hola " + invitation.getFirstName().trim();
        String expiresAt = formattedExpiresAt(invitation.getExpiresAt());

        return """
                %s,

                %s te invito a Recred para gestionar la cuenta escolar de tus hijos.

                Abri este link:
                %s

                El link vence el %s.

                Si no esperabas esta invitacion, podes ignorar este mensaje.
                """.formatted(greeting, school, invitationLink, expiresAt);
    }

    private String htmlBody(TutorInvitation invitation, String invitationLink) {
        String school = invitation.getSchool() == null
                ? "Tu colegio"
                : invitation.getSchool().getNombre();
        String greeting = invitation.getFirstName() == null || invitation.getFirstName().isBlank()
                ? "Hola"
                : "Hola " + invitation.getFirstName().trim();
        String expiresAt = formattedExpiresAt(invitation.getExpiresAt());

        return """
                <!doctype html>
                <html lang="es">
                  <body style="font-family: Arial, sans-serif; color: #1f2937;">
                    <h2 style="margin-bottom: 12px;">%s</h2>
                    <p>%s te invito a Recred para gestionar la cuenta escolar de tus hijos.</p>
                    <p style="margin: 24px 0;">
                      <a href="%s"
                         style="background: #2563eb; color: #ffffff; padding: 12px 18px;
                                text-decoration: none; border-radius: 6px; display: inline-block;">
                        Abrir Recred
                      </a>
                    </p>
                    <p>El link vence el <strong>%s</strong>.</p>
                    <p style="font-size: 12px; color: #6b7280;">
                      Si el boton no funciona, copia este link en tu navegador:<br>
                      <a href="%s">%s</a>
                    </p>
                    <p style="font-size: 12px; color: #6b7280;">
                      Si no esperabas esta invitacion, podes ignorar este mensaje.
                    </p>
                  </body>
                </html>
                """.formatted(
                escapeHtml(greeting),
                escapeHtml(school),
                escapeHtml(invitationLink),
                escapeHtml(expiresAt),
                escapeHtml(invitationLink),
                escapeHtml(invitationLink));
    }

    private String formattedExpiresAt(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            return "fecha no disponible";
        }
        return EXPIRATION_FORMAT.format(expiresAt);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
