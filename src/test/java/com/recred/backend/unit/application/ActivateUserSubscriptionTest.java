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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivateUserSubscriptionTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final SuscripcionUsuarioRepository subscriptionRepository =
            mock(SuscripcionUsuarioRepository.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-07-05T13:00:00Z"),
            ZoneOffset.UTC
    );
    private final ActivateUserSubscription useCase = new ActivateUserSubscription(
            usuarioRepository,
            subscriptionRepository,
            clock
    );

    @Test
    void execute_WhenUserCanSubscribe_ShouldActivateSubscriptionAndUserPlan() {
        UUID userId = UUID.randomUUID();
        givenUser(user(userId, RolUsuario.PADRE));
        givenNoActiveSubscription(userId);
        givenSavedSubscriptionWithId(UUID.randomUUID());
        ActivateUserSubscriptionRequest request = request(userId, TipoPlan.INTERMEDIO);

        ActivateUserSubscriptionResponse result = whenExecuting(request);

        assertEquals(TipoPlan.INTERMEDIO, result.plan());
        assertEquals(EstadoSuscripcion.ACTIVA, result.estado());
        assertEquals(fixedNow().plusMonths(1), result.fechaVencimiento());
        verifyUserPlanWasUpdated(userId, TipoPlan.INTERMEDIO);
        verifyNewSubscriptionWasSaved();
    }

    @Test
    void execute_WhenUserHasActiveSubscription_ShouldCancelPreviousSubscription() {
        UUID userId = UUID.randomUUID();
        SuscripcionUsuario activeSubscription = activeSubscription(userId);
        givenUser(user(userId, RolUsuario.VENDEDOR));
        givenActiveSubscription(userId, activeSubscription);
        givenSavedSubscriptionWithId(UUID.randomUUID());
        ActivateUserSubscriptionRequest request = request(userId, TipoPlan.AVANZADO);

        whenExecuting(request);

        List<SuscripcionUsuario> savedSubscriptions = savedSubscriptions();
        assertEquals(EstadoSuscripcion.CANCELADA, savedSubscriptions.get(0).getEstado());
        assertEquals(EstadoSuscripcion.ACTIVA, savedSubscriptions.get(1).getEstado());
    }

    @Test
    void execute_WhenUserDoesNotExist_ShouldThrowException() {
        UUID userId = UUID.randomUUID();
        givenMissingUser(userId);
        ActivateUserSubscriptionRequest request = request(userId, TipoPlan.INTERMEDIO);

        assertThrows(UsuarioNoEncontradoException.class, () -> whenExecuting(request));
        verify(subscriptionRepository, never()).save(any(SuscripcionUsuario.class));
        verify(usuarioRepository, never()).updatePlan(any(UUID.class), any(TipoPlan.class));
    }

    @Test
    void execute_WhenUserRoleCannotSubscribe_ShouldThrowException() {
        UUID userId = UUID.randomUUID();
        givenUser(user(userId, RolUsuario.ALUMNO));
        ActivateUserSubscriptionRequest request = request(userId, TipoPlan.INTERMEDIO);

        assertThrows(UserRoleCannotSubscribeException.class, () -> whenExecuting(request));
        verify(subscriptionRepository, never()).save(any(SuscripcionUsuario.class));
        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(usuarioRepository, never()).updatePlan(any(UUID.class), any(TipoPlan.class));
    }

    private void givenUser(Usuario user) {
        when(usuarioRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
    }

    private void givenMissingUser(UUID userId) {
        when(usuarioRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());
    }

    private void givenNoActiveSubscription(UUID userId) {
        when(subscriptionRepository.findActiveByUsuarioId(userId)).thenReturn(Optional.empty());
    }

    private void givenActiveSubscription(UUID userId, SuscripcionUsuario subscription) {
        when(subscriptionRepository.findActiveByUsuarioId(userId))
                .thenReturn(Optional.of(subscription));
    }

    private void givenSavedSubscriptionWithId(UUID subscriptionId) {
        when(subscriptionRepository.save(any(SuscripcionUsuario.class))).thenAnswer(invocation -> {
            SuscripcionUsuario subscription = invocation.getArgument(0);
            if (subscription.getId() == null) {
                subscription.setId(subscriptionId);
            }
            return subscription;
        });
    }

    private ActivateUserSubscriptionResponse whenExecuting(
            ActivateUserSubscriptionRequest request
    ) {
        return useCase.execute(request);
    }

    private void verifyUserPlanWasUpdated(UUID userId, TipoPlan plan) {
        verify(usuarioRepository).updatePlan(userId, plan);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    private void verifyNewSubscriptionWasSaved() {
        verify(subscriptionRepository).save(org.mockito.ArgumentMatchers.argThat(
                subscription -> subscription.getEstado() == EstadoSuscripcion.ACTIVA
        ));
    }

    private List<SuscripcionUsuario> savedSubscriptions() {
        ArgumentCaptor<SuscripcionUsuario> captor =
                ArgumentCaptor.forClass(SuscripcionUsuario.class);
        verify(subscriptionRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        return captor.getAllValues();
    }

    private ActivateUserSubscriptionRequest request(UUID userId, TipoPlan plan) {
        return new ActivateUserSubscriptionRequest(
                userId,
                plan,
                PeriodoSuscripcion.MENSUAL,
                new BigDecimal("5500.00"),
                null
        );
    }

    private Usuario user(UUID userId, RolUsuario role) {
        Usuario user = new Usuario();
        user.setId(userId);
        user.setRol(role);
        user.setPlan(TipoPlan.GRATUITO);
        return user;
    }

    private SuscripcionUsuario activeSubscription(UUID userId) {
        SuscripcionUsuario subscription = SuscripcionUsuario.activate(
                userId,
                TipoPlan.INTERMEDIO,
                PeriodoSuscripcion.MENSUAL,
                new BigDecimal("5500.00"),
                fixedNow().minusDays(5)
        );
        subscription.setId(UUID.randomUUID());
        return subscription;
    }

    private LocalDateTime fixedNow() {
        return LocalDateTime.now(clock);
    }
}
