package com.recred.backend.infrastructure.scheduling;

import com.recred.backend.application.GenerateParentSummary;
import com.recred.backend.application.GenerateParentSummaryRequest;
import com.recred.backend.application.GenerateParentSummaryResponse;
import com.recred.backend.application.GenerateSummaryNotification;
import com.recred.backend.application.GenerateSummaryNotificationRequest;
import com.recred.backend.application.GenerateSummaryNotificationResponse;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.services.PushNotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class GenerateParentSummaryJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateParentSummaryJob.class);

    private final GenerateParentSummary generateParentSummary;
    private final GenerateSummaryNotification generateSummaryNotification;
    private final PushNotificationSender pushNotificationSender;

    public GenerateParentSummaryJob(
            GenerateParentSummary generateParentSummary,
            GenerateSummaryNotification generateSummaryNotification,
            PushNotificationSender pushNotificationSender
    ) {
        this.generateParentSummary = generateParentSummary;
        this.generateSummaryNotification = generateSummaryNotification;
        this.pushNotificationSender = pushNotificationSender;
    }

    @Scheduled(cron = "00 00 13 * * SAT")
    public void run() {
        LocalDateTime fin = LocalDateTime.now();
        LocalDateTime inicio = fin.minusDays(7);

        try {
            GenerateParentSummaryRequest request = new GenerateParentSummaryRequest(inicio, fin);
            GenerateParentSummaryResponse response = generateParentSummary.execute(request);

            for (java.util.UUID userId : response.processedUserIds()) {
                notifyUser(userId);
            }
        } catch (Exception e) {
            LOGGER.error("Error al ejecutar GenerateParentSummaryJob: {}", e.getMessage(), e);
        }
    }

    private void notifyUser(java.util.UUID userId) {
        try {
            GenerateSummaryNotificationRequest request =
                    new GenerateSummaryNotificationRequest(userId);
            GenerateSummaryNotificationResponse response =
                    generateSummaryNotification.execute(request);

            PushNotification notification = new PushNotification(
                    response.userId(),
                    response.title(),
                    response.message(),
                    Map.of("type", response.type())
            );
            pushNotificationSender.send(notification);
        } catch (Exception e) {
            LOGGER.error("Error al enviar notificación para el usuario {}: {}",
                    userId, e.getMessage(), e);
        }
    }
}
