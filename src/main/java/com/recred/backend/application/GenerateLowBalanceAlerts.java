package com.recred.backend.application;

import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.AlumnoRepository;
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

    public void execute() {
        LOGGER.info("Starting low balance alert generation...");
        List<UUID> studentsWithLowBalance = 
                walletRepository.findAlumnoIdsWithLowBalance(LOW_BALANCE_THRESHOLD);
        
        LOGGER.info("Found {} students with low balance.", studentsWithLowBalance.size());

        for (UUID studentId : studentsWithLowBalance) {
            UUID studentUserId = alumnoRepository.findUserID(studentId);
            if (studentUserId != null) {
                LOGGER.info("Sending notification to student user id: {}", studentUserId);
                sendNotification(studentUserId, ALERT_TITLE, ALERT_MESSAGE_STUDENT);
            }

            List<Tutor> tutors = tutorRepository.findByAlumnoId(studentId);
            LOGGER.info("Found {} tutors for student id: {}", tutors.size(), studentId);
            for (Tutor tutor : tutors) {
                if (tutor.getUsuario() != null && tutor.getUsuario().getId() != null) {
                    sendNotification(tutor.getUsuario().getId(), ALERT_TITLE, ALERT_MESSAGE_PARENT);
                }
            }
        }
    }

    private void sendNotification(UUID userId, String title, String message) {
        PushNotification notification = new PushNotification(
                userId,
                title,
                message,
                Map.of("type", "LOW_BALANCE_ALERT")
        );
        pushNotificationSender.send(notification);
    }
}
