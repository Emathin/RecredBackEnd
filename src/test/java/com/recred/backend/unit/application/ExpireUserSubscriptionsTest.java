package com.recred.backend.unit.application;

import com.recred.backend.application.ExpireUserSubscriptions;
import com.recred.backend.application.ExpireUserSubscriptionsRequest;
import com.recred.backend.application.ExpireUserSubscriptionsResponse;
import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.SuscripcionUsuario;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.SuscripcionUsuarioRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpireUserSubscriptionsTest {

    private final SuscripcionUsuarioRepository subscriptionRepository =
            mock(SuscripcionUsuarioRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-07-05T13:00:00Z"),
            ZoneOffset.UTC
    );
    private final ExpireUserSubscriptions useCase = new ExpireUserSubscriptions(
            subscriptionRepository,
            usuarioRepository,
            clock
    );

    @Test
    void execute_WhenActiveSubscriptionsExpired_ShouldExpireAndReturnUsersToFreePlan() {
        UUID userId = UUID.randomUUID();
        LocalDateTime targetTime = fixedNow();
        SuscripcionUsuario subscription = expiredSubscription(userId, targetTime);
        Usuario user = user(userId, TipoPlan.AVANZADO);
        givenExpiredSubscriptions(targetTime, List.of(subscription));
        givenUser(user);

        ExpireUserSubscriptionsResponse result = whenExecuting(targetTime);

        assertEquals(1, result.expiredSubscriptions());
        assertEquals(EstadoSuscripcion.VENCIDA, subscription.getEstado());
        verify(subscriptionRepository).save(subscription);
        verify(usuarioRepository).updatePlan(userId, TipoPlan.GRATUITO);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void execute_WhenThereAreNoExpiredSubscriptions_ShouldReturnZero() {
        LocalDateTime targetTime = fixedNow();
        givenExpiredSubscriptions(targetTime, List.of());

        ExpireUserSubscriptionsResponse result = whenExecuting(targetTime);

        assertEquals(0, result.expiredSubscriptions());
        verify(subscriptionRepository, never()).save(any(SuscripcionUsuario.class));
        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(usuarioRepository, never()).updatePlan(any(UUID.class), any(TipoPlan.class));
    }

    private void givenExpiredSubscriptions(
            LocalDateTime targetTime,
            List<SuscripcionUsuario> subscriptions
    ) {
        when(subscriptionRepository.findActiveExpiredAt(targetTime)).thenReturn(subscriptions);
    }

    private void givenUser(Usuario user) {
        when(usuarioRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    private ExpireUserSubscriptionsResponse whenExecuting(LocalDateTime targetTime) {
        return useCase.execute(new ExpireUserSubscriptionsRequest(targetTime));
    }

    private SuscripcionUsuario expiredSubscription(UUID userId, LocalDateTime targetTime) {
        return SuscripcionUsuario.activate(
                userId,
                TipoPlan.AVANZADO,
                PeriodoSuscripcion.MENSUAL,
                new BigDecimal("10200.00"),
                targetTime.minusMonths(1)
        );
    }

    private Usuario user(UUID userId, TipoPlan plan) {
        Usuario user = new Usuario();
        user.setId(userId);
        user.setRol(RolUsuario.PADRE);
        user.setPlan(plan);
        return user;
    }

    private LocalDateTime fixedNow() {
        return LocalDateTime.now(clock);
    }
}
