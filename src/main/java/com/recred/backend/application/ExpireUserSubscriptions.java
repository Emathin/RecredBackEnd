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
    private final UsuarioRepository userRepository;
    private final TutorChildrenPlanPropagator tutorChildrenPlanPropagator;
    private final Clock clock;

    public ExpireUserSubscriptions(
            SuscripcionUsuarioRepository subscriptionRepository,
            UsuarioRepository userRepository,
            TutorChildrenPlanPropagator tutorChildrenPlanPropagator,
            Clock clock
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.tutorChildrenPlanPropagator = tutorChildrenPlanPropagator;
        this.clock = clock;
    }

    @Transactional
    public ExpireUserSubscriptionsResponse execute(
            ExpireUserSubscriptionsRequest expirationRequest
    ) {
        LocalDateTime referenceTime = referenceTimeFrom(expirationRequest);
        List<SuscripcionUsuario> expiredSubscriptions =
                activeSubscriptionsExpiredAt(referenceTime);
        return expireSubscriptions(expiredSubscriptions, referenceTime);
    }

    private LocalDateTime referenceTimeFrom(ExpireUserSubscriptionsRequest expirationRequest) {
        if (expirationRequest != null && expirationRequest.targetTime() != null) {
            return expirationRequest.targetTime();
        }

        return LocalDateTime.now(clock);
    }

    private List<SuscripcionUsuario> activeSubscriptionsExpiredAt(
            LocalDateTime referenceTime
    ) {
        return subscriptionRepository.findActiveExpiredAt(referenceTime);
    }

    private ExpireUserSubscriptionsResponse expireSubscriptions(
            List<SuscripcionUsuario> subscriptions,
            LocalDateTime referenceTime
    ) {
        subscriptions.forEach(subscription -> expireSubscriptionAt(
                subscription,
                referenceTime));
        return new ExpireUserSubscriptionsResponse(subscriptions.size());
    }

    private void expireSubscriptionAt(
            SuscripcionUsuario subscription,
            LocalDateTime referenceTime
    ) {
        subscription.expireAt(referenceTime);
        subscriptionRepository.save(subscription);
        returnSubscriptionOwnerToFreePlan(subscription);
    }

    private void returnSubscriptionOwnerToFreePlan(SuscripcionUsuario subscription) {
        userRepository.findById(subscription.getUsuarioId())
                .ifPresent(this::returnUserToFreePlan);
    }

    private void returnUserToFreePlan(Usuario user) {
        user.returnToFreePlan();
        userRepository.updatePlan(user.getId(), user.getPlan());
        tutorChildrenPlanPropagator.propagatePlanChangeFor(user);
    }
}
