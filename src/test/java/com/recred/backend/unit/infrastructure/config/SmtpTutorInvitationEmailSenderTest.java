package com.recred.backend.unit.infrastructure.config;

import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.infrastructure.email.SmtpMailProperties;
import com.recred.backend.infrastructure.email.SmtpTutorInvitationEmailSender;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

class SmtpTutorInvitationEmailSenderTest {

    private static final String INVITATION_LINK = "https://recred.test/invitacion?token=abc";

    @Test
    void sessionShouldUseConfiguredSmtpPropertiesAndAuthenticator() throws Exception {
        SmtpTutorInvitationEmailSender sender = sender(authenticatedProperties());

        Session session = invoke(sender, "session", Session.class);
        Object authenticator = invoke(sender, "authenticator", Object.class);
        String fromAddress = invoke(sender, "fromAddress", String.class);

        assertEquals("smtp.test", session.getProperty("mail.smtp.host"));
        assertEquals(2525, session.getProperties().get("mail.smtp.port"));
        assertEquals("true", session.getProperty("mail.smtp.auth"));
        assertEquals("true", session.getProperty("mail.smtp.starttls.enable"));
        assertEquals("smtp.test", session.getProperty("mail.smtp.ssl.trust"));
        assertEquals("1000", session.getProperty("mail.smtp.connectiontimeout"));
        assertEquals("2000", session.getProperty("mail.smtp.timeout"));
        assertEquals("3000", session.getProperty("mail.smtp.writetimeout"));
        assertInstanceOf(Authenticator.class, authenticator);
        PasswordAuthentication authentication = invoke(
                authenticator,
                "getPasswordAuthentication",
                PasswordAuthentication.class
        );
        assertEquals("smtp-user", authentication.getUserName());
        assertEquals("smtp-password", authentication.getPassword());
        assertEquals("noreply@recred.test", fromAddress);
    }

    @Test
    void sessionShouldOmitOptionalSslTrustAndAuthenticatorWhenAuthIsDisabled()
            throws Exception {
        SmtpTutorInvitationEmailSender sender = sender(properties(
                null,
                false,
                false,
                " "
        ));

        Session session = invoke(sender, "session", Session.class);
        Object authenticator = invoke(sender, "authenticator", Object.class);
        String fromAddress = invoke(sender, "fromAddress", String.class);

        assertNull(session.getProperty("mail.smtp.ssl.trust"));
        assertEquals("false", session.getProperty("mail.smtp.auth"));
        assertNull(authenticator);
        assertEquals("smtp-user", fromAddress);
    }

    @Test
    void contentShouldBuildTextAndHtmlAlternativeParts() throws Exception {
        SmtpTutorInvitationEmailSender sender = sender(authenticatedProperties());
        TutorInvitation invitation = invitation(" Ana ", school("Colegio Central"));

        Multipart multipart = invoke(
                sender,
                "content",
                Multipart.class,
                new Class<?>[] {TutorInvitation.class, String.class},
                invitation,
                INVITATION_LINK
        );

        assertEquals(2, multipart.getCount());
        assertTrue(String.valueOf(multipart.getBodyPart(0).getContent())
                .contains("Hola Ana"));
        assertTrue(String.valueOf(multipart.getBodyPart(0).getContent())
                .contains("Colegio Central te invito a Recred"));
        assertTrue(String.valueOf(multipart.getBodyPart(0).getContent())
                .contains("El link vence el 5/7/2026 12:00."));
        assertTrue(String.valueOf(multipart.getBodyPart(1).getContent())
                .contains("Abrir Recred"));
        assertTrue(String.valueOf(multipart.getBodyPart(1).getContent())
                .contains(INVITATION_LINK));
        assertTrue(String.valueOf(multipart.getBodyPart(1).getContent())
                .contains("5/7/2026 12:00"));
        assertFalse(String.valueOf(multipart.getBodyPart(1).getContent())
                .contains("2026-07-05T12:00"));
    }

    @Test
    void bodyBuildersShouldUseFallbacksAndEscapeHtmlValues() throws Exception {
        SmtpTutorInvitationEmailSender sender = sender(authenticatedProperties());
        TutorInvitation invitation = invitation(" ", null);
        String unsafeLink = "https://recred.test/?token=a&b=<c>\"'";

        String textBody = invoke(
                sender,
                "textBody",
                String.class,
                new Class<?>[] {TutorInvitation.class, String.class},
                invitation,
                unsafeLink
        );
        String htmlBody = invoke(
                sender,
                "htmlBody",
                String.class,
                new Class<?>[] {TutorInvitation.class, String.class},
                invitation,
                unsafeLink
        );

        assertTrue(textBody.contains("Hola,"));
        assertTrue(textBody.contains("Tu colegio te invito a Recred"));
        assertTrue(textBody.contains(unsafeLink));
        assertTrue(htmlBody.contains("Tu colegio"));
        assertTrue(htmlBody.contains("a&amp;b=&lt;c&gt;&quot;&#39;"));
    }

    @Test
    void escapeHtmlShouldReturnEmptyStringForNullValue() throws Exception {
        SmtpTutorInvitationEmailSender sender = sender(authenticatedProperties());

        String escaped = invoke(
                sender,
                "escapeHtml",
                String.class,
                new Class<?>[] {String.class},
                new Object[] {null}
        );

        assertEquals("", escaped);
    }

    @Test
    void sendInvitationShouldBuildAndSendMessage() {
        SmtpTutorInvitationEmailSender sender = sender(authenticatedProperties());

        try (MockedStatic<Transport> transport = mockStatic(Transport.class)) {
            sender.sendInvitation(invitation("Ana", school("Colegio Central")),
                    INVITATION_LINK);

            transport.verify(() -> Transport.send(any(Message.class)));
        }
    }

    @Test
    void sendInvitationShouldWrapMessagingException() throws Exception {
        SmtpTutorInvitationEmailSender sender = sender(authenticatedProperties());

        try (MockedStatic<Transport> transport = mockStatic(Transport.class)) {
            transport.when(() -> Transport.send(any(Message.class)))
                    .thenThrow(new MessagingException("smtp down"));

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> sender.sendInvitation(
                            invitation("Ana", school("Colegio Central")),
                            INVITATION_LINK
                    )
            );

            assertEquals("No se pudo enviar el email de invitacion al tutor",
                    exception.getMessage());
        }
    }

    private SmtpTutorInvitationEmailSender sender(SmtpMailProperties properties) {
        return new SmtpTutorInvitationEmailSender(properties);
    }

    private SmtpMailProperties authenticatedProperties() {
        return properties(
                "noreply@recred.test",
                true,
                true,
                "smtp.test"
        );
    }

    private SmtpMailProperties properties(
            String from,
            boolean auth,
            boolean startTls,
            String sslTrust
    ) {
        return new SmtpMailProperties(
                "smtp.test",
                2525,
                "smtp-user",
                "smtp-password",
                from,
                auth,
                startTls,
                sslTrust,
                1000,
                2000,
                3000
        );
    }

    private TutorInvitation invitation(String firstName, Colegio school) {
        TutorInvitation invitation = new TutorInvitation();
        invitation.setEmail("tutor@recred.test");
        invitation.setFirstName(firstName);
        invitation.setSchool(school);
        invitation.setExpiresAt(LocalDateTime.of(2026, 7, 5, 12, 0));
        return invitation;
    }

    private Colegio school(String name) {
        Colegio school = new Colegio();
        school.setNombre(name);
        return school;
    }

    private <T> T invoke(
            Object target,
            String methodName,
            Class<T> returnType
    ) throws Exception {
        return invoke(target, methodName, returnType, new Class<?>[] {});
    }

    private <T> T invoke(
            Object target,
            String methodName,
            Class<T> returnType,
            Class<?>[] parameterTypes,
            Object... args
    ) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        Object result = method.invoke(target, args);
        if (result == null) {
            return null;
        }
        return returnType.cast(result);
    }
}
