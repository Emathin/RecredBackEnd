package com.recred.backend.unit.application;

import com.recred.backend.application.ExpireUserSubscriptions;
import com.recred.backend.application.ExpireUserSubscriptionsRequest;
import com.recred.backend.application.ExpireUserSubscriptionsResponse;
import com.recred.backend.application.TutorChildrenPlanPropagator;
import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.SuscripcionUsuario;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.SuscripcionUsuarioRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpireUserSubscriptionsTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-05T13:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private SuscripcionUsuarioRepository subscriptionRepository;

    @Mock
    private UsuarioRepository userRepository;

    @Mock
    private TutorChildrenPlanPropagator tutorChildrenPlanPropagator;

    private ExpireUserSubscriptions useCase;

    private UUID userId;
    private LocalDateTime referenceTime;
    private SuscripcionUsuario subscription;
    private Usuario user;
    private ExpireUserSubscriptionsResponse response;

    @BeforeEach
    void setUp() {
        useCase = new ExpireUserSubscriptions(
                subscriptionRepository,
                userRepository,
                tutorChildrenPlanPropagator,
                CLOCK
        );
    }

    @Test
    void givenExpiredSubscription_whenExpiringSubscriptions_thenExpiresAndFreesUserPlan() {
        givenExpirationScenario();
        givenExpiredSubscription();
        givenSubscriptionOwnerWithPaidPlan();
        givenExpiredSubscriptions(subscription);

        whenExpiringUserSubscriptions();

        thenOneSubscriptionWasExpired();
        thenSubscriptionStateIsExpired();
        thenSubscriptionWasSaved();
        thenUserReturnedToFreePlan();
        thenPlanChangeWasPropagated();
        thenUserWasNotSavedDirectly();
    }

    @Test
    void givenNoExpiredSubscriptions_whenExpiringSubscriptions_thenReturnsZero() {
        givenExpirationScenario();
        givenExpiredSubscriptions();

        whenExpiringUserSubscriptions();

        thenNoSubscriptionsWereExpired();
        thenNoSubscriptionWasSaved();
        thenNoUserPlanWasUpdated();
        thenPlanChangeWasNotPropagated();
    }

    private void givenExpirationScenario() {
        userId = UUID.randomUUID();
        referenceTime = fixedNow();
    }

    private void givenExpiredSubscription() {
        subscription = SuscripcionUsuario.activate(
                userId,
                TipoPlan.AVANZADO,
                PeriodoSuscripcion.MENSUAL,
                new BigDecimal("10200.00"),
                referenceTime.minusMonths(1)
        );
    }

    private void givenSubscriptionOwnerWithPaidPlan() {
        user = new Usuario();
        user.setId(userId);
        user.setRol(RolUsuario.PADRE);
        user.setPlan(TipoPlan.AVANZADO);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }

    private void givenExpiredSubscriptions(SuscripcionUsuario... subscriptions) {
        when(subscriptionRepository.findActiveExpiredAt(referenceTime))
                .thenReturn(List.of(subscriptions));
    }

    private void whenExpiringUserSubscriptions() {
        response = useCase.execute(new ExpireUserSubscriptionsRequest(referenceTime));
    }

    private void thenOneSubscriptionWasExpired() {
        assertEquals(1, response.expiredSubscriptions());
    }

    private void thenNoSubscriptionsWereExpired() {
        assertEquals(0, response.expiredSubscriptions());
    }

    private void thenSubscriptionStateIsExpired() {
        assertEquals(EstadoSuscripcion.VENCIDA, subscription.getEstado());
    }

    private void thenSubscriptionWasSaved() {
        verify(subscriptionRepository).save(subscription);
    }

    private void thenUserReturnedToFreePlan() {
        verify(userRepository).updatePlan(userId, TipoPlan.GRATUITO);
    }

    private void thenPlanChangeWasPropagated() {
        verify(tutorChildrenPlanPropagator).propagatePlanChangeFor(user);
    }

    private void thenPlanChangeWasNotPropagated() {
        verify(tutorChildrenPlanPropagator, never()).propagatePlanChangeFor(any(Usuario.class));
    }

    private void thenUserWasNotSavedDirectly() {
        verify(userRepository, never()).save(any(Usuario.class));
    }

    private void thenNoSubscriptionWasSaved() {
        verify(subscriptionRepository, never()).save(any(SuscripcionUsuario.class));
    }

    private void thenNoUserPlanWasUpdated() {
        verify(userRepository, never()).save(any(Usuario.class));
        verify(userRepository, never()).updatePlan(any(UUID.class), any(TipoPlan.class));
    }

    private LocalDateTime fixedNow() {
        return LocalDateTime.now(CLOCK);
    }
}
