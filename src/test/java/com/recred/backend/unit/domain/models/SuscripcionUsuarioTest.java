package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.exceptions.InvalidSubscriptionPlanException;
import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.SuscripcionUsuario;
import com.recred.backend.domain.models.TipoPlan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuscripcionUsuarioTest {

    @Test
    void activate_WhenPeriodIsMonthly_ShouldCreateActiveSubscription() {
        LocalDateTime startDate = subscriptionStart();

        SuscripcionUsuario result = whenActivating(
                TipoPlan.INTERMEDIO,
                PeriodoSuscripcion.MENSUAL,
                startDate
        );

        assertEquals(EstadoSuscripcion.ACTIVA, result.getEstado());
        assertEquals(startDate.plusMonths(1), result.getFechaVencimiento());
        assertEquals(TipoPlan.INTERMEDIO, result.getPlan());
    }

    @Test
    void activate_WhenPeriodIsAnnual_ShouldCreateAnnualExpiration() {
        LocalDateTime startDate = subscriptionStart();

        SuscripcionUsuario result = whenActivating(
                TipoPlan.AVANZADO,
                PeriodoSuscripcion.ANUAL,
                startDate
        );

        assertEquals(startDate.plusYears(1), result.getFechaVencimiento());
    }

    @Test
    void activate_WhenPlanIsFree_ShouldRejectSubscription() {
        LocalDateTime startDate = subscriptionStart();

        assertThrows(
                InvalidSubscriptionPlanException.class,
                () -> whenActivating(TipoPlan.GRATUITO, PeriodoSuscripcion.MENSUAL, startDate)
        );
    }

    @Test
    void isActiveAt_WhenSubscriptionIsInsideValidRange_ShouldReturnTrue() {
        SuscripcionUsuario subscription = givenActiveSubscription();

        boolean result = whenCheckingActiveAt(
                subscription,
                subscription.getFechaInicio().plusDays(10)
        );

        assertTrue(result);
    }

    @Test
    void isExpiredAt_WhenExpirationWasReached_ShouldReturnTrue() {
        SuscripcionUsuario subscription = givenActiveSubscription();

        boolean result = whenCheckingExpiredAt(
                subscription,
                subscription.getFechaVencimiento()
        );

        assertTrue(result);
    }

    @Test
    void isExpiringSoonAt_WhenExpirationIsInsideWindow_ShouldReturnTrue() {
        SuscripcionUsuario subscription = givenActiveSubscription();

        boolean result = whenCheckingExpiringSoonAt(
                subscription,
                subscription.getFechaVencimiento().minusDays(3),
                7
        );

        assertTrue(result);
    }

    @Test
    void isExpiringSoonAt_WhenExpirationIsOutsideWindow_ShouldReturnFalse() {
        SuscripcionUsuario subscription = givenActiveSubscription();

        boolean result = whenCheckingExpiringSoonAt(
                subscription,
                subscription.getFechaInicio().plusDays(1),
                7
        );

        assertFalse(result);
    }

    @Test
    void shouldSendExpirationWarningAt_WhenMonthlyExpiresInSevenDays_ShouldReturnTrue() {
        SuscripcionUsuario subscription = givenActiveSubscription();

        boolean result = whenCheckingShouldSendExpirationWarningAt(
                subscription,
                subscription.getFechaVencimiento().minusDays(7)
        );

        assertTrue(result);
    }

    @Test
    void shouldSendExpirationWarningAt_WhenMonthlyExpiresAfterSevenDays_ShouldReturnFalse() {
        SuscripcionUsuario subscription = givenActiveSubscription();

        boolean result = whenCheckingShouldSendExpirationWarningAt(
                subscription,
                subscription.getFechaVencimiento().minusDays(8)
        );

        assertFalse(result);
    }

    @Test
    void shouldSendExpirationWarningAt_WhenAnnualExpiresInThirtyDays_ShouldReturnTrue() {
        SuscripcionUsuario subscription = whenActivating(
                TipoPlan.AVANZADO,
                PeriodoSuscripcion.ANUAL,
                subscriptionStart()
        );

        boolean result = whenCheckingShouldSendExpirationWarningAt(
                subscription,
                subscription.getFechaVencimiento().minusDays(30)
        );

        assertTrue(result);
    }

    @Test
    void markExpirationWarningSentAt_ShouldAvoidDuplicateWarnings() {
        SuscripcionUsuario subscription = givenActiveSubscription();
        LocalDateTime referenceDate = subscription.getFechaVencimiento().minusDays(7);

        whenMarkingExpirationWarningSentAt(subscription, referenceDate);
        boolean result = whenCheckingShouldSendExpirationWarningAt(subscription, referenceDate);

        assertFalse(result);
        assertEquals(referenceDate, subscription.getUltimoAvisoVencimientoEn());
    }

    @Test
    void expireAt_WhenSubscriptionExpired_ShouldChangeStatusToExpired() {
        SuscripcionUsuario subscription = givenActiveSubscription();

        whenExpiringAt(subscription, subscription.getFechaVencimiento());

        assertEquals(EstadoSuscripcion.VENCIDA, subscription.getEstado());
    }

    @Test
    void cancel_WhenSubscriptionIsActive_ShouldChangeStatusToCancelled() {
        SuscripcionUsuario subscription = givenActiveSubscription();

        whenCancelling(subscription);

        assertEquals(EstadoSuscripcion.CANCELADA, subscription.getEstado());
    }

    private SuscripcionUsuario givenActiveSubscription() {
        return whenActivating(
                TipoPlan.AVANZADO,
                PeriodoSuscripcion.MENSUAL,
                subscriptionStart()
        );
    }

    private SuscripcionUsuario whenActivating(
            TipoPlan plan,
            PeriodoSuscripcion period,
            LocalDateTime startDate
    ) {
        return SuscripcionUsuario.activate(
                UUID.randomUUID(),
                plan,
                period,
                new BigDecimal("5500.00"),
                startDate
        );
    }

    private boolean whenCheckingActiveAt(
            SuscripcionUsuario subscription,
            LocalDateTime referenceDate
    ) {
        return subscription.isActiveAt(referenceDate);
    }

    private boolean whenCheckingExpiredAt(
            SuscripcionUsuario subscription,
            LocalDateTime referenceDate
    ) {
        return subscription.isExpiredAt(referenceDate);
    }

    private boolean whenCheckingExpiringSoonAt(
            SuscripcionUsuario subscription,
            LocalDateTime referenceDate,
            int warningDays
    ) {
        return subscription.isExpiringSoonAt(referenceDate, warningDays);
    }

    private boolean whenCheckingShouldSendExpirationWarningAt(
            SuscripcionUsuario subscription,
            LocalDateTime referenceDate
    ) {
        return subscription.shouldSendExpirationWarningAt(referenceDate);
    }

    private void whenMarkingExpirationWarningSentAt(
            SuscripcionUsuario subscription,
            LocalDateTime referenceDate
    ) {
        subscription.markExpirationWarningSentAt(referenceDate);
    }

    private void whenExpiringAt(
            SuscripcionUsuario subscription,
            LocalDateTime referenceDate
    ) {
        subscription.expireAt(referenceDate);
    }

    private void whenCancelling(SuscripcionUsuario subscription) {
        subscription.cancel();
    }

    private LocalDateTime subscriptionStart() {
        return LocalDateTime.of(2026, 7, 5, 10, 0);
    }
}
