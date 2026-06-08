package com.recred.backend.infrastructure.scheduling;

import com.recred.backend.application.GenerateParentSummary;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.services.PushNotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class GenerateParentSummaryJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateParentSummaryJob.class);

    private final TutorRepository tutorRepository;
    private final GenerateParentSummary generateParentSummary;
    private final PushNotificationSender pushNotificationSender;

    public GenerateParentSummaryJob(
            TutorRepository tutorRepository,
            GenerateParentSummary generateParentSummary,
            PushNotificationSender pushNotificationSender
    ) {
        this.tutorRepository = tutorRepository;
        this.generateParentSummary = generateParentSummary;
        this.pushNotificationSender = pushNotificationSender;
    }

    @Scheduled(cron = "0 30 12 * * SAT")
    public void run() {
        List<Tutor> tutors = tutorRepository.findAll();
        LocalDateTime fin = LocalDateTime.now();
        LocalDateTime inicio = fin.minusDays(7);

        for (Tutor tutor : tutors) {
            try {
                generateParentSummary.execute(tutor.getUsuario().getId(), inicio, fin);

                PushNotification notification = new PushNotification(
                        tutor.getUsuario().getId(),
                        "Resumen Semanal",
                        "Tu resumen semanal de gastos ya está disponible.",
                        null
                );
                pushNotificationSender.send(notification);

            } catch (Exception e) {
                LOGGER.error("Error al generar resumen o enviar notificación para el tutor {}: {}",
                        tutor.getUsuario().getId(), e.getMessage(), e);
            }
        }
    }
}
