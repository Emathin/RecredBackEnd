package com.recred.backend.application;

import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.models.SuscripcionColegio;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.SuscripcionColegioRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

public class ActivateSchoolSubscription {

    private final ColegioRepository colegioRepository;
    private final SuscripcionColegioRepository subscriptionRepository;
    private final Clock clock;

    public ActivateSchoolSubscription(
            ColegioRepository colegioRepository,
            SuscripcionColegioRepository subscriptionRepository,
            Clock clock
    ) {
        this.colegioRepository = colegioRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.clock = clock;
    }

    @Transactional
    public ActivateSchoolSubscriptionResponse execute(
            ActivateSchoolSubscriptionRequest activationRequest
    ) {
        ensureSchoolExistsAndIsActive(activationRequest.colegioId());
        SuscripcionColegio subscription = activeSubscriptionOrNew(activationRequest);
        renewExistingSubscriptionIfPresent(subscription, activationRequest);
        return ActivateSchoolSubscriptionResponse.fromDomain(saveSubscription(subscription));
    }

    private void ensureSchoolExistsAndIsActive(UUID schoolId) {
        if (!colegioRepository.existsActiveById(schoolId)) {
            throw new SchoolNotFoundException(schoolId);
        }
    }

    private SuscripcionColegio activeSubscriptionOrNew(
            ActivateSchoolSubscriptionRequest activationRequest
    ) {
        return subscriptionRepository.findActiveByColegioId(activationRequest.colegioId())
                .orElseGet(() -> newSubscriptionFrom(activationRequest));
    }

    private SuscripcionColegio newSubscriptionFrom(
            ActivateSchoolSubscriptionRequest activationRequest
    ) {
        return SuscripcionColegio.activate(
                activationRequest.colegioId(),
                activationRequest.precio(),
                activationRequest.moneda(),
                startDateFrom(activationRequest)
        );
    }

    private LocalDateTime startDateFrom(ActivateSchoolSubscriptionRequest activationRequest) {
        if (activationRequest.fechaInicio() != null) {
            return activationRequest.fechaInicio();
        }

        return LocalDateTime.now(clock);
    }

    private void renewExistingSubscriptionIfPresent(
            SuscripcionColegio subscription,
            ActivateSchoolSubscriptionRequest activationRequest
    ) {
        if (subscription.getId() == null) {
            return;
        }

        subscription.renew(
                activationRequest.precio(),
                activationRequest.moneda(),
                startDateFrom(activationRequest));
    }

    private SuscripcionColegio saveSubscription(SuscripcionColegio subscription) {
        return subscriptionRepository.save(subscription);
    }
}
