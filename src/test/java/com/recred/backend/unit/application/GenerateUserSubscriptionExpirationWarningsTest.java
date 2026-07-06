package com.recred.backend.unit.application;

import com.recred.backend.application.GenerateUserSubscriptionExpirationWarnings;
import com.recred.backend.application.GenerateUserSubscriptionExpirationWarningsRequest;
import com.recred.backend.application.GenerateUserSubscriptionExpirationWarningsResponse;
import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.SuscripcionUsuario;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.repositories.NotificationRepository;
import com.recred.backend.domain.repositories.SuscripcionUsuarioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenerateUserSubscriptionExpirationWarningsTest {

    private final SuscripcionUsuarioRepository subscriptionRepository =
            mock(SuscripcionUsuarioRepository.class);
    private final NotificationRepository notificationRepository =
            mock(NotificationRepository.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-07-05T13:00:00Z"),
            ZoneOffset.UTC
    );
    private final GenerateUserSubscriptionExpirationWarnings useCase =
            new GenerateUserSubscriptionExpirationWarnings(
                    subscriptionRepository,
                    notificationRepository,
                    clock
            );

    @Test
    void execute_WhenMonthlySubscriptionExpiresInSevenDays_ShouldNotifyUser() {
        LocalDateTime targetTime = fixedNow();
        UUID userId = UUID.randomUUID();
        SuscripcionUsuario subscription = monthlySubscription(userId, targetTime.plusDays(7));
        givenExpiringSubscriptions(targetTime, List.of(subscription));

        GenerateUserSubscriptionExpirationWarningsResponse result = whenExecuting(targetTime);

        assertEquals(1, result.sentNotifications());
        Notification notification = capturedNotification(userId);
        assertEquals(TipoNotificacion.SISTEMA, notification.type());
        assertEquals("Tu suscripcion esta por vencer", notification.title());
        assertNotNull(subscription.getUltimoAvisoVencimientoEn());
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void execute_WhenAnnualSubscriptionExpiresInThirtyDays_ShouldNotifyUser() {
        LocalDateTime targetTime = fixedNow();
        UUID userId = UUID.randomUUID();
        SuscripcionUsuario subscription = annualSubscription(userId, targetTime.plusDays(30));
        givenExpiringSubscriptions(targetTime, List.of(subscription));

        GenerateUserSubscriptionExpirationWarningsResponse result = whenExecuting(targetTime);

        assertEquals(1, result.sentNotifications());
        verify(notificationRepository).save(
                any(Notification.class),
                org.mockito.Mockito.eq(userId));
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void execute_WhenMonthlySubscriptionExpiresAfterSevenDays_ShouldNotNotifyUser() {
        LocalDateTime targetTime = fixedNow();
        SuscripcionUsuario subscription =
                monthlySubscription(UUID.randomUUID(), targetTime.plusDays(8));
        givenExpiringSubscriptions(targetTime, List.of(subscription));

        GenerateUserSubscriptionExpirationWarningsResponse result = whenExecuting(targetTime);

        assertEquals(0, result.sentNotifications());
        verify(notificationRepository, never()).save(any(Notification.class), any(UUID.class));
        verify(subscriptionRepository, never()).save(any(SuscripcionUsuario.class));
    }

    @Test
    void execute_WhenSubscriptionWasAlreadyWarned_ShouldNotNotifyAgain() {
        LocalDateTime targetTime = fixedNow();
        SuscripcionUsuario subscription =
                monthlySubscription(UUID.randomUUID(), targetTime.plusDays(7));
        subscription.markExpirationWarningSentAt(targetTime);
        givenExpiringSubscriptions(targetTime, List.of(subscription));

        GenerateUserSubscriptionExpirationWarningsResponse result = whenExecuting(targetTime);

        assertEquals(0, result.sentNotifications());
        verify(notificationRepository, never()).save(any(Notification.class), any(UUID.class));
        verify(subscriptionRepository, never()).save(any(SuscripcionUsuario.class));
    }

    private void givenExpiringSubscriptions(
            LocalDateTime targetTime,
            List<SuscripcionUsuario> subscriptions
    ) {
        when(subscriptionRepository.findActiveExpiringBetween(
                targetTime,
                targetTime.plusDays(30)
        )).thenReturn(subscriptions);
    }

    private GenerateUserSubscriptionExpirationWarningsResponse whenExecuting(
            LocalDateTime targetTime
    ) {
        return useCase.execute(new GenerateUserSubscriptionExpirationWarningsRequest(targetTime));
    }

    private Notification capturedNotification(UUID userId) {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture(), org.mockito.Mockito.eq(userId));
        return captor.getValue();
    }

    private SuscripcionUsuario monthlySubscription(UUID userId, LocalDateTime expirationDate) {
        return subscription(
                userId,
                PeriodoSuscripcion.MENSUAL,
                expirationDate.minusMonths(1)
        );
    }

    private SuscripcionUsuario annualSubscription(UUID userId, LocalDateTime expirationDate) {
        return subscription(
                userId,
                PeriodoSuscripcion.ANUAL,
                expirationDate.minusYears(1)
        );
    }

    private SuscripcionUsuario subscription(
            UUID userId,
            PeriodoSuscripcion period,
            LocalDateTime startDate
    ) {
        return SuscripcionUsuario.activate(
                userId,
                TipoPlan.AVANZADO,
                period,
                new BigDecimal("10200.00"),
                startDate
        );
    }

    private LocalDateTime fixedNow() {
        return LocalDateTime.now(clock);
    }
}
