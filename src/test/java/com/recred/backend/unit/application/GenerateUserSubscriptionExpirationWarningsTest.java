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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateUserSubscriptionExpirationWarningsTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-05T13:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private SuscripcionUsuarioRepository subscriptionRepository;

    @Mock
    private NotificationRepository notificationRepository;

    private GenerateUserSubscriptionExpirationWarnings useCase;

    private UUID userId;
    private LocalDateTime referenceTime;
    private SuscripcionUsuario subscription;
    private GenerateUserSubscriptionExpirationWarningsResponse response;

    @BeforeEach
    void setUp() {
        useCase = new GenerateUserSubscriptionExpirationWarnings(
                subscriptionRepository,
                notificationRepository,
                CLOCK
        );
    }

    @Test
    void givenMonthlySubscriptionExpiresInSevenDays_whenGeneratingWarnings_thenNotifiesUser() {
        givenWarningScenario();
        givenMonthlySubscriptionExpiresIn(7);
        givenExpiringSubscriptions(subscription);

        whenGeneratingExpirationWarnings();

        thenOneWarningWasSent();
        thenSystemNotificationWasSavedForUser();
        thenSubscriptionWasMarkedAsWarned();
    }

    @Test
    void givenAnnualSubscriptionExpiresInThirtyDays_whenGeneratingWarnings_thenNotifiesUser() {
        givenWarningScenario();
        givenAnnualSubscriptionExpiresIn(30);
        givenExpiringSubscriptions(subscription);

        whenGeneratingExpirationWarnings();

        thenOneWarningWasSent();
        thenNotificationWasSavedForUser();
        thenSubscriptionWasSaved();
    }

    @Test
    void givenMonthlySubscriptionExpiresAfterSevenDays_whenGeneratingWarnings_thenDoesNotNotify() {
        givenWarningScenario();
        givenMonthlySubscriptionExpiresIn(8);
        givenExpiringSubscriptions(subscription);

        whenGeneratingExpirationWarnings();

        thenNoWarningsWereSent();
        thenNoNotificationWasSaved();
        thenNoSubscriptionWasSaved();
    }

    @Test
    void givenSubscriptionWasAlreadyWarned_whenGeneratingWarnings_thenDoesNotNotifyAgain() {
        givenWarningScenario();
        givenMonthlySubscriptionExpiresIn(7);
        givenSubscriptionWasAlreadyWarned();
        givenExpiringSubscriptions(subscription);

        whenGeneratingExpirationWarnings();

        thenNoWarningsWereSent();
        thenNoNotificationWasSaved();
        thenNoSubscriptionWasSaved();
    }

    private void givenWarningScenario() {
        userId = UUID.randomUUID();
        referenceTime = fixedNow();
    }

    private void givenMonthlySubscriptionExpiresIn(int days) {
        subscription = subscription(
                PeriodoSuscripcion.MENSUAL,
                referenceTime.plusDays(days).minusMonths(1)
        );
    }

    private void givenAnnualSubscriptionExpiresIn(int days) {
        subscription = subscription(
                PeriodoSuscripcion.ANUAL,
                referenceTime.plusDays(days).minusYears(1)
        );
    }

    private void givenSubscriptionWasAlreadyWarned() {
        subscription.markExpirationWarningSentAt(referenceTime);
    }

    private void givenExpiringSubscriptions(SuscripcionUsuario... subscriptions) {
        when(subscriptionRepository.findActiveExpiringBetween(
                referenceTime,
                referenceTime.plusDays(30)
        )).thenReturn(List.of(subscriptions));
    }

    private void whenGeneratingExpirationWarnings() {
        response = useCase.execute(
                new GenerateUserSubscriptionExpirationWarningsRequest(referenceTime));
    }

    private void thenOneWarningWasSent() {
        assertEquals(1, response.sentNotifications());
    }

    private void thenNoWarningsWereSent() {
        assertEquals(0, response.sentNotifications());
    }

    private void thenSystemNotificationWasSavedForUser() {
        Notification notification = savedNotification();
        assertEquals(TipoNotificacion.SISTEMA, notification.type());
        assertEquals("Tu suscripcion esta por vencer", notification.title());
    }

    private void thenNotificationWasSavedForUser() {
        verify(notificationRepository).save(any(Notification.class), eq(userId));
    }

    private void thenSubscriptionWasMarkedAsWarned() {
        assertNotNull(subscription.getUltimoAvisoVencimientoEn());
        thenSubscriptionWasSaved();
    }

    private void thenSubscriptionWasSaved() {
        verify(subscriptionRepository).save(subscription);
    }

    private void thenNoNotificationWasSaved() {
        verify(notificationRepository, never()).save(any(Notification.class), any(UUID.class));
    }

    private void thenNoSubscriptionWasSaved() {
        verify(subscriptionRepository, never()).save(any(SuscripcionUsuario.class));
    }

    private Notification savedNotification() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture(), eq(userId));
        return captor.getValue();
    }

    private SuscripcionUsuario subscription(
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
        return LocalDateTime.now(CLOCK);
    }
}
