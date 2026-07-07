package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.EstadoLicenciaColegio;
import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.domain.models.SuscripcionColegio;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuscripcionColegioTest {

    @Test
    void activate_ShouldCreateMonthlySubscriptionWithGracePeriod() {
        LocalDateTime startDate = startDate();

        SuscripcionColegio result = whenActivating(startDate);

        assertEquals(EstadoSuscripcion.ACTIVA, result.getEstado());
        assertEquals(startDate.plusMonths(1), result.getFechaVencimiento());
        assertEquals(startDate.plusMonths(2), result.getFechaGraciaHasta());
        assertEquals("USD", result.getMoneda());
    }

    @Test
    void canUseAppAt_WhenSubscriptionIsPaidActive_ShouldReturnTrue() {
        SuscripcionColegio subscription = givenActiveSubscription();

        boolean result = whenCheckingAccessAt(subscription, startDate().plusDays(10));

        assertTrue(result);
    }

    @Test
    void canUseAppAt_WhenSubscriptionIsInGracePeriod_ShouldReturnTrue() {
        SuscripcionColegio subscription = givenActiveSubscription();

        boolean result = whenCheckingAccessAt(subscription, startDate().plusMonths(1).plusDays(5));

        assertTrue(result);
        assertTrue(subscription.isInGraceAt(startDate().plusMonths(1).plusDays(5)));
    }

    @Test
    void canUseAppAt_WhenGracePeriodExpired_ShouldReturnFalse() {
        SuscripcionColegio subscription = givenActiveSubscription();

        boolean result = whenCheckingAccessAt(subscription, startDate().plusMonths(2));

        assertFalse(result);
    }

    @Test
    void estadoLicenciaAt_WhenSubscriptionIsPaidActive_ShouldReturnActive() {
        SuscripcionColegio subscription = givenActiveSubscription();

        EstadoLicenciaColegio result = whenGettingLicenseStateAt(
                subscription,
                startDate().plusDays(10));

        assertEquals(EstadoLicenciaColegio.ACTIVA, result);
    }

    @Test
    void estadoLicenciaAt_WhenSubscriptionIsInGracePeriod_ShouldReturnGrace() {
        SuscripcionColegio subscription = givenActiveSubscription();

        EstadoLicenciaColegio result = whenGettingLicenseStateAt(
                subscription,
                startDate().plusMonths(1).plusDays(5));

        assertEquals(EstadoLicenciaColegio.EN_GRACIA, result);
    }

    @Test
    void estadoLicenciaAt_WhenGracePeriodExpired_ShouldReturnExpired() {
        SuscripcionColegio subscription = givenActiveSubscription();

        EstadoLicenciaColegio result = whenGettingLicenseStateAt(
                subscription,
                startDate().plusMonths(2));

        assertEquals(EstadoLicenciaColegio.VENCIDA, result);
    }

    @Test
    void estadoLicenciaAt_WhenSubscriptionIsCancelled_ShouldReturnCancelled() {
        SuscripcionColegio subscription = givenActiveSubscription();
        subscription.cancel();

        EstadoLicenciaColegio result = whenGettingLicenseStateAt(
                subscription,
                startDate().plusDays(10));

        assertEquals(EstadoLicenciaColegio.CANCELADA, result);
    }

    @Test
    void expireAt_WhenGracePeriodExpired_ShouldChangeStatusToExpired() {
        SuscripcionColegio subscription = givenActiveSubscription();

        whenExpiringAt(subscription, startDate().plusMonths(2));

        assertEquals(EstadoSuscripcion.VENCIDA, subscription.getEstado());
    }

    @Test
    void renew_ShouldUpdateDatesAndReactivateSubscription() {
        SuscripcionColegio subscription = givenActiveSubscription();
        whenExpiringAt(subscription, startDate().plusMonths(2));
        LocalDateTime renewalDate = startDate().plusMonths(2).plusDays(1);

        whenRenewing(subscription, renewalDate);

        assertEquals(EstadoSuscripcion.ACTIVA, subscription.getEstado());
        assertEquals(renewalDate, subscription.getFechaInicio());
        assertEquals(renewalDate.plusMonths(1), subscription.getFechaVencimiento());
        assertEquals(renewalDate.plusMonths(2), subscription.getFechaGraciaHasta());
        assertEquals("USD", subscription.getMoneda());
    }

    @Test
    void cancel_WhenSubscriptionIsActive_ShouldChangeStatusToCancelled() {
        SuscripcionColegio subscription = givenActiveSubscription();

        whenCancelling(subscription);

        assertEquals(EstadoSuscripcion.CANCELADA, subscription.getEstado());
    }

    @Test
    void activate_WhenPriceIsInvalid_ShouldThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SuscripcionColegio.activate(
                        UUID.randomUUID(),
                        BigDecimal.ZERO,
                        "USD",
                        startDate()
                )
        );
    }

    private SuscripcionColegio givenActiveSubscription() {
        return whenActivating(startDate());
    }

    private SuscripcionColegio whenActivating(LocalDateTime startDate) {
        return SuscripcionColegio.activate(
                UUID.randomUUID(),
                new BigDecimal("20.00"),
                "USD",
                startDate
        );
    }

    private boolean whenCheckingAccessAt(
            SuscripcionColegio subscription,
            LocalDateTime referenceDate
    ) {
        return subscription.canUseAppAt(referenceDate);
    }

    private EstadoLicenciaColegio whenGettingLicenseStateAt(
            SuscripcionColegio subscription,
            LocalDateTime referenceDate
    ) {
        return subscription.estadoLicenciaAt(referenceDate);
    }

    private void whenExpiringAt(
            SuscripcionColegio subscription,
            LocalDateTime referenceDate
    ) {
        subscription.expireAt(referenceDate);
    }

    private void whenCancelling(SuscripcionColegio subscription) {
        subscription.cancel();
    }

    private void whenRenewing(SuscripcionColegio subscription, LocalDateTime renewalDate) {
        subscription.renew(new BigDecimal("20.00"), "USD", renewalDate);
    }

    private LocalDateTime startDate() {
        return LocalDateTime.of(2026, 7, 5, 10, 0);
    }
}
