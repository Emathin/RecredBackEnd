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
    public ActivateSchoolSubscriptionResponse execute(ActivateSchoolSubscriptionRequest request) {
        requireActiveSchool(request.colegioId());
        SuscripcionColegio subscription = activeSubscriptionOrNew(request);
        renew(subscription, request);
        return ActivateSchoolSubscriptionResponse.fromDomain(save(subscription));
    }

    private void requireActiveSchool(UUID colegioId) {
        if (!colegioRepository.existsActiveById(colegioId)) {
            throw new SchoolNotFoundException(colegioId);
        }
    }

    private SuscripcionColegio activeSubscriptionOrNew(
            ActivateSchoolSubscriptionRequest request
    ) {
        return subscriptionRepository.findActiveByColegioId(request.colegioId())
                .orElseGet(() -> newSubscription(request));
    }

    private SuscripcionColegio newSubscription(ActivateSchoolSubscriptionRequest request) {
        return SuscripcionColegio.activate(
                request.colegioId(),
                request.precio(),
                request.moneda(),
                startDate(request)
        );
    }

    private LocalDateTime startDate(ActivateSchoolSubscriptionRequest request) {
        if (request.fechaInicio() != null) {
            return request.fechaInicio();
        }

        return LocalDateTime.now(clock);
    }

    private void renew(
            SuscripcionColegio subscription,
            ActivateSchoolSubscriptionRequest request
    ) {
        if (subscription.getId() == null) {
            return;
        }

        subscription.renew(request.precio(), request.moneda(), startDate(request));
    }

    private SuscripcionColegio save(SuscripcionColegio subscription) {
        return subscriptionRepository.save(subscription);
    }
}
