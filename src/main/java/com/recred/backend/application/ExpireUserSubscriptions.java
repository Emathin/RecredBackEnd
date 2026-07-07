package com.recred.backend.application;

import com.recred.backend.domain.models.SuscripcionUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.SuscripcionUsuarioRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

public class ExpireUserSubscriptions {

    private final SuscripcionUsuarioRepository subscriptionRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public ExpireUserSubscriptions(
            SuscripcionUsuarioRepository subscriptionRepository,
            UsuarioRepository usuarioRepository,
            Clock clock
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    @Transactional
    public ExpireUserSubscriptionsResponse execute(ExpireUserSubscriptionsRequest request) {
        LocalDateTime targetTime = targetTime(request);
        return expire(expiredSubscriptions(targetTime), targetTime);
    }

    private LocalDateTime targetTime(ExpireUserSubscriptionsRequest request) {
        if (request != null && request.targetTime() != null) {
            return request.targetTime();
        }

        return LocalDateTime.now(clock);
    }

    private List<SuscripcionUsuario> expiredSubscriptions(LocalDateTime targetTime) {
        return subscriptionRepository.findActiveExpiredAt(targetTime);
    }

    private ExpireUserSubscriptionsResponse expire(
            List<SuscripcionUsuario> subscriptions,
            LocalDateTime targetTime
    ) {
        subscriptions.forEach(subscription -> expire(subscription, targetTime));
        return new ExpireUserSubscriptionsResponse(subscriptions.size());
    }

    private void expire(SuscripcionUsuario subscription, LocalDateTime targetTime) {
        subscription.expireAt(targetTime);
        subscriptionRepository.save(subscription);
        returnUserToFreePlan(subscription);
    }

    private void returnUserToFreePlan(SuscripcionUsuario subscription) {
        usuarioRepository.findById(subscription.getUsuarioId())
                .ifPresent(this::returnUserToFreePlan);
    }

    private void returnUserToFreePlan(Usuario user) {
        user.returnToFreePlan();
        usuarioRepository.updatePlan(user.getId(), user.getPlan());
    }
}
