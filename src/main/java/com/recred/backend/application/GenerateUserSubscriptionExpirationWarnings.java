package com.recred.backend.application;

import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.SuscripcionUsuario;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.domain.repositories.NotificationRepository;
import com.recred.backend.domain.repositories.SuscripcionUsuarioRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GenerateUserSubscriptionExpirationWarnings {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String TITLE = "Tu suscripcion esta por vencer";

    private final SuscripcionUsuarioRepository subscriptionRepository;
    private final NotificationRepository notificationRepository;
    private final Clock clock;

    public GenerateUserSubscriptionExpirationWarnings(
            SuscripcionUsuarioRepository subscriptionRepository,
            NotificationRepository notificationRepository,
            Clock clock
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.notificationRepository = notificationRepository;
        this.clock = clock;
    }

    @Transactional
    public GenerateUserSubscriptionExpirationWarningsResponse execute(
            GenerateUserSubscriptionExpirationWarningsRequest warningRequest
    ) {
        LocalDateTime referenceTime = referenceTimeFrom(warningRequest);
        List<SuscripcionUsuario> subscriptions =
                activeSubscriptionsNearExpirationAt(referenceTime);
        int sentNotifications = sendExpirationWarnings(subscriptions, referenceTime);
        return new GenerateUserSubscriptionExpirationWarningsResponse(sentNotifications);
    }

    private LocalDateTime referenceTimeFrom(
            GenerateUserSubscriptionExpirationWarningsRequest warningRequest
    ) {
        if (warningRequest != null && warningRequest.targetTime() != null) {
            return warningRequest.targetTime();
        }

        return LocalDateTime.now(clock);
    }

    private List<SuscripcionUsuario> activeSubscriptionsNearExpirationAt(
            LocalDateTime referenceTime
    ) {
        return subscriptionRepository.findActiveExpiringBetween(
                referenceTime,
                referenceTime.plusDays(PeriodoSuscripcion.ANUAL.expirationWarningDays())
        );
    }

    private int sendExpirationWarnings(
            List<SuscripcionUsuario> subscriptions,
            LocalDateTime referenceTime
    ) {
        return subscriptions.stream()
                .filter(subscription -> subscription.shouldSendExpirationWarningAt(referenceTime))
                .mapToInt(subscription -> sendExpirationWarning(subscription, referenceTime))
                .sum();
    }

    private int sendExpirationWarning(
            SuscripcionUsuario subscription,
            LocalDateTime referenceTime
    ) {
        notificationRepository.save(notificationFor(subscription, referenceTime),
                subscription.getUsuarioId());
        subscription.markExpirationWarningSentAt(referenceTime);
        subscriptionRepository.save(subscription);
        return 1;
    }

    private Notification notificationFor(
            SuscripcionUsuario subscription,
            LocalDateTime referenceTime
    ) {
        return new Notification(
                null,
                TipoNotificacion.SISTEMA,
                TITLE,
                messageFor(subscription, referenceTime),
                false,
                referenceTime,
                null,
                null,
                null,
                null,
                null
        );
    }

    private String messageFor(SuscripcionUsuario subscription, LocalDateTime referenceTime) {
        long days = daysUntilExpiration(subscription, referenceTime);
        return "Tu plan " + subscription.getPlan().name()
                + " vence el " + subscription.getFechaVencimiento().format(DATE_FORMATTER)
                + ". Te quedan " + days
                + " dias para renovarlo y mantener tus beneficios.";
    }

    private long daysUntilExpiration(
            SuscripcionUsuario subscription,
            LocalDateTime referenceTime
    ) {
        return Duration.between(referenceTime, subscription.getFechaVencimiento()).toDays();
    }
}
