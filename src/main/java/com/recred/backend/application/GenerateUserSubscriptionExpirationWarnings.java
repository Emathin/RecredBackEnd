package com.recred.backend.application;

import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.SuscripcionUsuario;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.domain.repositories.NotificationRepository;
import com.recred.backend.domain.repositories.SuscripcionUsuarioRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
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
            GenerateUserSubscriptionExpirationWarningsRequest request
    ) {
        LocalDateTime targetTime = targetTime(request);
        int sentNotifications = sendWarnings(expiringSubscriptions(targetTime), targetTime);
        return new GenerateUserSubscriptionExpirationWarningsResponse(sentNotifications);
    }

    private LocalDateTime targetTime(GenerateUserSubscriptionExpirationWarningsRequest request) {
        if (request != null && request.targetTime() != null) {
            return request.targetTime();
        }

        return LocalDateTime.now(clock);
    }

    private List<SuscripcionUsuario> expiringSubscriptions(LocalDateTime targetTime) {
        return subscriptionRepository.findActiveExpiringBetween(
                targetTime,
                targetTime.plusDays(PeriodoSuscripcion.ANUAL.expirationWarningDays())
        );
    }

    private int sendWarnings(List<SuscripcionUsuario> subscriptions, LocalDateTime targetTime) {
        return subscriptions.stream()
                .filter(subscription -> subscription.shouldSendExpirationWarningAt(targetTime))
                .mapToInt(subscription -> sendWarning(subscription, targetTime))
                .sum();
    }

    private int sendWarning(SuscripcionUsuario subscription, LocalDateTime targetTime) {
        notificationRepository.save(notification(subscription, targetTime),
                subscription.getUsuarioId());
        subscription.markExpirationWarningSentAt(targetTime);
        subscriptionRepository.save(subscription);
        return 1;
    }

    private Notification notification(
            SuscripcionUsuario subscription,
            LocalDateTime targetTime
    ) {
        return new Notification(
                null,
                TipoNotificacion.SISTEMA,
                TITLE,
                message(subscription, targetTime),
                false,
                targetTime,
                null,
                null,
                null,
                null,
                null
        );
    }

    private String message(SuscripcionUsuario subscription, LocalDateTime targetTime) {
        long days = java.time.Duration.between(
                targetTime,
                subscription.getFechaVencimiento()
        ).toDays();
        return "Tu plan " + subscription.getPlan().name()
                + " vence el " + subscription.getFechaVencimiento().format(DATE_FORMATTER)
                + ". Te quedan " + days
                + " dias para renovarlo y mantener tus beneficios.";
    }
}
