package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.exceptions.InvalidSubscriptionPlanException;
import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.SuscripcionUsuario;
import com.recred.backend.domain.models.TipoPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.recred.backend.domain.models.PeriodoSuscripcion.ANUAL;
import static com.recred.backend.domain.models.PeriodoSuscripcion.MENSUAL;
import static com.recred.backend.domain.models.PeriodoSuscripcion.PRUEBA;
import static com.recred.backend.domain.models.SuscripcionUsuario.activate;
import static com.recred.backend.domain.models.TipoPlan.AVANZADO;
import static com.recred.backend.domain.models.TipoPlan.GRATUITO;
import static com.recred.backend.domain.models.TipoPlan.INTERMEDIO;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuscripcionUsuarioTest {

    private UUID usuarioId;
    private TipoPlan plan;
    private PeriodoSuscripcion periodo;
    private BigDecimal precio;
    private LocalDateTime fechaInicio;
    private SuscripcionUsuario suscripcion;
    private boolean checkResult;
    private RuntimeException exception;

    @Test
    void activate_WhenPeriodIsMonthly_ShouldCreateActiveSubscription() {
        givenDefaultUser();
        givenPlan(INTERMEDIO);
        givenPeriod(MENSUAL);
        givenPrice("5500.00");
        givenStartDate(subscriptionStart());

        whenActivating();

        thenSubscriptionIsActive();
        thenExpirationDateIs(fechaInicio.plusMonths(1));
        thenPlanIs(INTERMEDIO);
    }

    @Test
    void activate_WhenPeriodIsAnnual_ShouldCreateAnnualExpiration() {
        givenDefaultUser();
        givenPlan(AVANZADO);
        givenPeriod(ANUAL);
        givenPrice("5500.00");
        givenStartDate(subscriptionStart());

        whenActivating();

        thenExpirationDateIs(fechaInicio.plusYears(1));
    }

    @Test
    void activate_WhenPlanIsFree_ShouldRejectSubscription() {
        givenDefaultUser();
        givenPlan(GRATUITO);
        givenPeriod(MENSUAL);
        givenPrice("5500.00");
        givenStartDate(subscriptionStart());

        whenActivatingThrows(InvalidSubscriptionPlanException.class);
    }

    @Test
    void isActiveAt_WhenSubscriptionIsInsideValidRange_ShouldReturnTrue() {
        givenActiveSubscription();

        whenCheckingActiveAt(suscripcion.getFechaInicio().plusDays(10));

        thenCheckResultIsTrue();
    }

    @Test
    void isExpiredAt_WhenExpirationWasReached_ShouldReturnTrue() {
        givenActiveSubscription();

        whenCheckingExpiredAt(suscripcion.getFechaVencimiento());

        thenCheckResultIsTrue();
    }

    @Test
    void isExpiringSoonAt_WhenExpirationIsInsideWindow_ShouldReturnTrue() {
        givenActiveSubscription();

        whenCheckingExpiringSoonAt(suscripcion.getFechaVencimiento().minusDays(3), 7);

        thenCheckResultIsTrue();
    }

    @Test
    void isExpiringSoonAt_WhenExpirationIsOutsideWindow_ShouldReturnFalse() {
        givenActiveSubscription();

        whenCheckingExpiringSoonAt(suscripcion.getFechaInicio().plusDays(1), 7);

        thenCheckResultIsFalse();
    }

    @Test
    void shouldSendExpirationWarningAt_WhenMonthlyExpiresInSevenDays_ShouldReturnTrue() {
        givenActiveSubscription();

        whenCheckingShouldSendExpirationWarningAt(suscripcion.getFechaVencimiento().minusDays(7));

        thenCheckResultIsTrue();
    }

    @Test
    void shouldSendExpirationWarningAt_WhenMonthlyExpiresAfterSevenDays_ShouldReturnFalse() {
        givenActiveSubscription();

        whenCheckingShouldSendExpirationWarningAt(suscripcion.getFechaVencimiento().minusDays(8));

        thenCheckResultIsFalse();
    }

    @Test
    void shouldSendExpirationWarningAt_WhenAnnualExpiresInThirtyDays_ShouldReturnTrue() {
        givenAnnualSubscription();

        whenCheckingShouldSendExpirationWarningAt(suscripcion.getFechaVencimiento().minusDays(30));

        thenCheckResultIsTrue();
    }

    @Test
    void markExpirationWarningSentAt_ShouldAvoidDuplicateWarnings() {
        givenActiveSubscription();
        LocalDateTime warningDate = suscripcion.getFechaVencimiento().minusDays(7);

        whenMarkingWarningSentAt(warningDate);
        whenCheckingShouldSendExpirationWarningAt(warningDate);

        thenCheckResultIsFalse();
        thenLastWarningDateIs(warningDate);
    }

    @Test
    void expireAt_WhenSubscriptionExpired_ShouldChangeStatusToExpired() {
        givenActiveSubscription();

        whenExpiringAt(suscripcion.getFechaVencimiento());

        thenStatusIs(EstadoSuscripcion.VENCIDA);
    }

    @Test
    void cancel_WhenSubscriptionIsActive_ShouldChangeStatusToCancelled() {
        givenActiveSubscription();

        whenCancelling();

        thenStatusIs(EstadoSuscripcion.CANCELADA);
    }

    @Test
    void activate_WhenPeriodIsTrialAndPriceIsZero_ShouldCreateActiveSubscription() {
        givenDefaultUser();
        givenPlan(INTERMEDIO);
        givenPeriod(PRUEBA);
        givenPrice("0.00");
        givenStartDate(subscriptionStart());

        whenActivating();

        thenSubscriptionIsActive();
        thenExpirationDateIs(fechaInicio.plusMonths(1));
        thenPriceIs("0.00");
    }

    @Test
    void activate_WhenPeriodIsTrialAndPriceIsPositive_ShouldThrowException() {
        givenDefaultUser();
        givenPlan(INTERMEDIO);
        givenPeriod(PRUEBA);
        givenPrice("1000.00");
        givenStartDate(subscriptionStart());

        whenActivatingThrows(IllegalArgumentException.class);
    }

    @Test
    void activate_WhenPeriodIsNotTrialAndPriceIsZero_ShouldThrowException() {
        givenDefaultUser();
        givenPlan(INTERMEDIO);
        givenPeriod(MENSUAL);
        givenPrice("0.00");
        givenStartDate(subscriptionStart());

        whenActivatingThrows(IllegalArgumentException.class);
    }

    private void givenDefaultUser() {
        usuarioId = randomUUID();
    }

    private void givenPlan(TipoPlan plan) {
        this.plan = plan;
    }

    private void givenPeriod(PeriodoSuscripcion periodo) {
        this.periodo = periodo;
    }

    private void givenPrice(String price) {
        this.precio = new BigDecimal(price);
    }

    private void givenStartDate(LocalDateTime startDate) {
        this.fechaInicio = startDate;
    }

    private void givenActiveSubscription() {
        suscripcion = activate(
                randomUUID(),
                AVANZADO,
                MENSUAL,
                new BigDecimal("5500.00"),
                subscriptionStart()
        );
    }

    private void givenAnnualSubscription() {
        suscripcion = activate(
                randomUUID(),
                AVANZADO,
                ANUAL,
                new BigDecimal("5500.00"),
                subscriptionStart()
        );
    }

    private void whenActivating() {
        suscripcion = activate(usuarioId, plan, periodo, precio, fechaInicio);
    }

    private void whenActivatingThrows(Class<? extends RuntimeException> expectedException) {
        Executable execution = this::whenActivating;
        exception = assertThrows(expectedException, execution);
    }

    private void whenCheckingActiveAt(LocalDateTime referenceDate) {
        checkResult = suscripcion.isActiveAt(referenceDate);
    }

    private void whenCheckingExpiredAt(LocalDateTime referenceDate) {
        checkResult = suscripcion.isExpiredAt(referenceDate);
    }

    private void whenCheckingExpiringSoonAt(LocalDateTime referenceDate, int warningDays) {
        checkResult = suscripcion.isExpiringSoonAt(referenceDate, warningDays);
    }

    private void whenCheckingShouldSendExpirationWarningAt(LocalDateTime referenceDate) {
        checkResult = suscripcion.shouldSendExpirationWarningAt(referenceDate);
    }

    private void whenMarkingWarningSentAt(LocalDateTime referenceDate) {
        suscripcion.markExpirationWarningSentAt(referenceDate);
    }

    private void whenExpiringAt(LocalDateTime referenceDate) {
        suscripcion.expireAt(referenceDate);
    }

    private void whenCancelling() {
        suscripcion.cancel();
    }

    private void thenSubscriptionIsActive() {
        assertEquals(EstadoSuscripcion.ACTIVA, suscripcion.getEstado());
    }

    private void thenExpirationDateIs(LocalDateTime expectedDate) {
        assertEquals(expectedDate, suscripcion.getFechaVencimiento());
    }

    private void thenPlanIs(TipoPlan expectedPlan) {
        assertEquals(expectedPlan, suscripcion.getPlan());
    }

    private void thenPriceIs(String expectedPrice) {
        assertEquals(new BigDecimal(expectedPrice), suscripcion.getPrecio());
    }

    private void thenCheckResultIsTrue() {
        assertTrue(checkResult);
    }

    private void thenCheckResultIsFalse() {
        assertFalse(checkResult);
    }

    private void thenLastWarningDateIs(LocalDateTime expectedDate) {
        assertEquals(expectedDate, suscripcion.getUltimoAvisoVencimientoEn());
    }

    private void thenStatusIs(EstadoSuscripcion expectedStatus) {
        assertEquals(expectedStatus, suscripcion.getEstado());
    }

    private LocalDateTime subscriptionStart() {
        return LocalDateTime.of(2026, 7, 5, 10, 0);
    }
}
