package com.recred.backend.application;

import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.repositories.AlumnoRepository;

import static com.recred.backend.domain.models.RolUsuario.ALUMNO;
import static com.recred.backend.domain.models.RolUsuario.PADRE;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.PushNotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GenerateLowBalanceAlerts {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateLowBalanceAlerts.class);

    private final WalletRepository walletRepository;
    private final AlumnoRepository alumnoRepository;
    private final TutorRepository tutorRepository;
    private final PushNotificationSender pushNotificationSender;

    private static final BigDecimal LOW_BALANCE_THRESHOLD = new BigDecimal("5000.00");
    private static final String ALERT_TITLE = "Saldo bajo";
    private static final String ALERT_MESSAGE_STUDENT = 
            "Tu saldo es bajo. Por favor recarga tu cuenta.";
    private static final String ALERT_MESSAGE_PARENT = 
            "El saldo de tu hijo es bajo. Por favor recarga su cuenta.";

    public GenerateLowBalanceAlerts(
            WalletRepository walletRepository,
            AlumnoRepository alumnoRepository,
            TutorRepository tutorRepository,
            PushNotificationSender pushNotificationSender) {
        this.walletRepository = walletRepository;
        this.alumnoRepository = alumnoRepository;
        this.tutorRepository = tutorRepository;
        this.pushNotificationSender = pushNotificationSender;
    }

    public GenerateLowBalanceAlertsResponse execute(GenerateLowBalanceAlertsRequest request) {
        LOGGER.info("Starting low balance alert generation...");
        List<UUID> studentsWithLowBalance = studentsWithLowBalance();
        LOGGER.info("Found {} students with low balance.", studentsWithLowBalance.size());
        studentsWithLowBalance.forEach(this::sendStudentAndTutorNotifications);
        return new GenerateLowBalanceAlertsResponse();
    }

    private List<UUID> studentsWithLowBalance() {
        return walletRepository.findAlumnoIdsWithLowBalance(LOW_BALANCE_THRESHOLD);
    }

    private void sendStudentAndTutorNotifications(UUID studentId) {
        sendStudentNotification(studentId);
        sendTutorNotifications(studentId);
    }

    private void sendStudentNotification(UUID studentId) {
        UUID studentUserId = alumnoRepository.findUserID(studentId);
        if (studentUserId == null) {
            return;
        }

        LOGGER.info("Sending notification to student user id: {}", studentUserId);
        sendNotification(
                studentUserId,
                ALUMNO,
                ALERT_TITLE,
                ALERT_MESSAGE_STUDENT,
                studentId);
    }

    private void sendTutorNotifications(UUID studentId) {
        List<Tutor> tutors = tutorRepository.findByAlumnoId(studentId);
        LOGGER.info("Found {} tutors for student id: {}", tutors.size(), studentId);
        tutors.stream()
                .filter(tutor -> tutor.getUsuario() != null)
                .filter(tutor -> tutor.getUsuario().getId() != null)
                .forEach(tutor -> sendNotification(
                        tutor.getUsuario().getId(),
                        PADRE,
                        ALERT_TITLE,
                        ALERT_MESSAGE_PARENT,
                        studentId
                ));
    }

    private void sendNotification(
        UUID userId, 
        RolUsuario rol, 
        String title, 
        String message, 
        UUID alumnoId
    ) {
        PushNotification notification = new PushNotification(
                userId,
                title,
                message,
                Map.of(
                    "type", "LOW_BALANCE_ALERT",
                    "alumnoId", alumnoId.toString(),
                    "rol", rol.toString()
                )
        );
        pushNotificationSender.send(notification);
    }
}
