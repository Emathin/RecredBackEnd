package com.recred.backend.application;

import com.recred.backend.domain.exceptions.UsuarioNoEncontradoException;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.SuscripcionUsuario;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.SuscripcionUsuarioRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

public class ActivateUserSubscription {

    private final UsuarioRepository usuarioRepository;
    private final SuscripcionUsuarioRepository subscriptionRepository;
    private final TutorChildrenPlanPropagator tutorChildrenPlanPropagator;
    private final Clock clock;

    public ActivateUserSubscription(
            UsuarioRepository usuarioRepository,
            SuscripcionUsuarioRepository subscriptionRepository,
            TutorChildrenPlanPropagator tutorChildrenPlanPropagator,
            Clock clock
    ) {
        this.usuarioRepository = usuarioRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.tutorChildrenPlanPropagator = tutorChildrenPlanPropagator;
        this.clock = clock;
    }

    @Transactional
    public ActivateUserSubscriptionResponse execute(
            ActivateUserSubscriptionRequest activationRequest
    ) {
        Usuario user = findUserAllowedToSubscribe(activationRequest.usuarioId());
        validateTrialEligibility(user, activationRequest.periodo());

        SuscripcionUsuario subscription = subscriptionFrom(activationRequest);
        cancelCurrentSubscription(user.getId());
        activateUserPlan(user, subscription.getPlan());
        return ActivateUserSubscriptionResponse.fromDomain(saveSubscription(subscription));
    }

    private void validateTrialEligibility(Usuario user, PeriodoSuscripcion period) {
        if (period == PeriodoSuscripcion.PRUEBA 
                && subscriptionRepository.hasUsedTrial(user.getId())) {
            throw new IllegalArgumentException(
                    "El usuario ya ha disfrutado de un periodo de prueba");
        }
    }

    private Usuario findUserAllowedToSubscribe(UUID userId) {
        Usuario user = usuarioRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UsuarioNoEncontradoException(userId));
        user.validateCanHaveIndividualSubscription();
        return user;
    }

    private SuscripcionUsuario subscriptionFrom(ActivateUserSubscriptionRequest activationRequest) {
        return SuscripcionUsuario.activate(
                activationRequest.usuarioId(),
                activationRequest.plan(),
                activationRequest.periodo(),
                activationRequest.precio(),
                startDateFrom(activationRequest)
        );
    }

    private LocalDateTime startDateFrom(ActivateUserSubscriptionRequest activationRequest) {
        if (activationRequest.fechaInicio() != null) {
            return activationRequest.fechaInicio();
        }

        return LocalDateTime.now(clock);
    }

    private void cancelCurrentSubscription(UUID usuarioId) {
        subscriptionRepository.findActiveByUsuarioId(usuarioId)
                .ifPresent(this::cancel);
    }

    private void cancel(SuscripcionUsuario subscription) {
        subscription.cancel();
        subscriptionRepository.save(subscription);
    }

    private void activateUserPlan(Usuario user, TipoPlan plan) {
        user.activatePaidPlan(plan);
        usuarioRepository.updatePlan(user.getId(), user.getPlan());
        tutorChildrenPlanPropagator.propagatePlanChangeFor(user);
    }

    private SuscripcionUsuario saveSubscription(SuscripcionUsuario subscription) {
        return subscriptionRepository.save(subscription);
    }
}
