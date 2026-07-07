package com.recred.backend.unit.application;

import com.recred.backend.application.ActivateUserSubscription;
import com.recred.backend.application.ActivateUserSubscriptionRequest;
import com.recred.backend.application.ActivateUserSubscriptionResponse;
import com.recred.backend.domain.exceptions.UserRoleCannotSubscribeException;
import com.recred.backend.domain.exceptions.UsuarioNoEncontradoException;
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
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivateUserSubscriptionTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-05T13:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private UsuarioRepository userRepository;

    @Mock
    private SuscripcionUsuarioRepository subscriptionRepository;

    private ActivateUserSubscription useCase;

    private UUID userId;
    private UUID subscriptionId;
    private Usuario user;
    private SuscripcionUsuario activeSubscription;
    private ActivateUserSubscriptionResponse response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new ActivateUserSubscription(
                userRepository,
                subscriptionRepository,
                CLOCK
        );
    }

    @Test
    void givenUserCanSubscribe_whenActivatingSubscription_thenActivatesSubscriptionAndPlan() {
        givenUserSubscriptionScenario(RolUsuario.PADRE, TipoPlan.INTERMEDIO);
        givenUserExists();
        givenNoActiveSubscription();
        givenSavedSubscriptionWithId();

        whenActivatingUserSubscription();

        thenSubscriptionWasActivated(TipoPlan.INTERMEDIO);
        thenUserPlanWasUpdated(TipoPlan.INTERMEDIO);
        thenNewSubscriptionWasSaved();
    }

    @Test
    void givenUserHasActiveSubscription_whenActivatingSubscription_thenCancelsPreviousOne() {
        givenUserSubscriptionScenario(RolUsuario.VENDEDOR, TipoPlan.AVANZADO);
        givenUserExists();
        givenActiveSubscriptionExists();
        givenSavedSubscriptionWithId();

        whenActivatingUserSubscription();

        thenPreviousSubscriptionWasCancelled();
        thenNewSubscriptionWasActivated();
    }

    @Test
    void givenUserDoesNotExist_whenActivatingSubscription_thenFailsBeforeSaving() {
        givenUserSubscriptionScenario(RolUsuario.PADRE, TipoPlan.INTERMEDIO);
        givenUserDoesNotExist();

        whenActivatingUserSubscriptionFails();

        thenUserWasNotFound();
        thenSubscriptionWasNotSaved();
        thenUserPlanWasNotUpdated();
    }

    @Test
    void givenUserRoleCannotSubscribe_whenActivatingSubscription_thenFailsBeforeSaving() {
        givenUserSubscriptionScenario(RolUsuario.ALUMNO, TipoPlan.INTERMEDIO);
        givenUserExists();

        whenActivatingUserSubscriptionFails();

        thenUserRoleCannotSubscribe();
        thenSubscriptionWasNotSaved();
        thenUserWasNotSaved();
        thenUserPlanWasNotUpdated();
    }

    private void givenUserSubscriptionScenario(RolUsuario role, TipoPlan plan) {
        userId = UUID.randomUUID();
        subscriptionId = UUID.randomUUID();
        user = user(userId, role);
        response = null;
        activeSubscription = activeSubscription(userId, plan);
    }

    private void givenUserExists() {
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
    }

    private void givenUserDoesNotExist() {
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());
    }

    private void givenNoActiveSubscription() {
        when(subscriptionRepository.findActiveByUsuarioId(userId)).thenReturn(Optional.empty());
    }

    private void givenActiveSubscriptionExists() {
        when(subscriptionRepository.findActiveByUsuarioId(userId))
                .thenReturn(Optional.of(activeSubscription));
    }

    private void givenSavedSubscriptionWithId() {
        when(subscriptionRepository.save(any(SuscripcionUsuario.class))).thenAnswer(invocation -> {
            SuscripcionUsuario subscription = invocation.getArgument(0);
            if (subscription.getId() == null) {
                subscription.setId(subscriptionId);
            }
            return subscription;
        });
    }

    private void whenActivatingUserSubscription() {
        response = useCase.execute(request(responsePlan()));
    }

    private void whenActivatingUserSubscriptionFails() {
        thrownException = assertThrows(
                RuntimeException.class,
                this::whenActivatingUserSubscription);
    }

    private void thenSubscriptionWasActivated(TipoPlan plan) {
        assertEquals(plan, response.plan());
        assertEquals(EstadoSuscripcion.ACTIVA, response.estado());
        assertEquals(fixedNow().plusMonths(1), response.fechaVencimiento());
    }

    private void thenUserPlanWasUpdated(TipoPlan plan) {
        verify(userRepository).updatePlan(userId, plan);
        verify(userRepository, never()).save(any(Usuario.class));
    }

    private void thenNewSubscriptionWasSaved() {
        verify(subscriptionRepository).save(org.mockito.ArgumentMatchers.argThat(
                subscription -> subscription.getEstado() == EstadoSuscripcion.ACTIVA
        ));
    }

    private void thenPreviousSubscriptionWasCancelled() {
        List<SuscripcionUsuario> savedSubscriptions = savedSubscriptions();
        assertEquals(EstadoSuscripcion.CANCELADA, savedSubscriptions.get(0).getEstado());
    }

    private void thenNewSubscriptionWasActivated() {
        List<SuscripcionUsuario> savedSubscriptions = savedSubscriptions();
        assertEquals(EstadoSuscripcion.ACTIVA, savedSubscriptions.get(1).getEstado());
    }

    private void thenUserWasNotFound() {
        assertTrue(thrownException instanceof UsuarioNoEncontradoException);
    }

    private void thenUserRoleCannotSubscribe() {
        assertTrue(thrownException instanceof UserRoleCannotSubscribeException);
    }

    private void thenSubscriptionWasNotSaved() {
        verify(subscriptionRepository, never()).save(any(SuscripcionUsuario.class));
    }

    private void thenUserWasNotSaved() {
        verify(userRepository, never()).save(any(Usuario.class));
    }

    private void thenUserPlanWasNotUpdated() {
        verify(userRepository, never()).updatePlan(any(UUID.class), any(TipoPlan.class));
    }

    private List<SuscripcionUsuario> savedSubscriptions() {
        ArgumentCaptor<SuscripcionUsuario> captor =
                ArgumentCaptor.forClass(SuscripcionUsuario.class);
        verify(subscriptionRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        return captor.getAllValues();
    }

    private ActivateUserSubscriptionRequest request(TipoPlan plan) {
        return new ActivateUserSubscriptionRequest(
                userId,
                plan,
                PeriodoSuscripcion.MENSUAL,
                new BigDecimal("5500.00"),
                null
        );
    }

    private TipoPlan responsePlan() {
        return user.getRol() == RolUsuario.VENDEDOR ? TipoPlan.AVANZADO : TipoPlan.INTERMEDIO;
    }

    private Usuario user(UUID userId, RolUsuario role) {
        Usuario user = new Usuario();
        user.setId(userId);
        user.setRol(role);
        user.setPlan(TipoPlan.GRATUITO);
        return user;
    }

    private SuscripcionUsuario activeSubscription(UUID userId, TipoPlan plan) {
        SuscripcionUsuario subscription = SuscripcionUsuario.activate(
                userId,
                plan,
                PeriodoSuscripcion.MENSUAL,
                new BigDecimal("5500.00"),
                fixedNow().minusDays(5)
        );
        subscription.setId(UUID.randomUUID());
        return subscription;
    }

    private LocalDateTime fixedNow() {
        return LocalDateTime.now(CLOCK);
    }
}
