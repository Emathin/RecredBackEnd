package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.models.SuscripcionColegio;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.SuscripcionColegioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class SchoolSubscriptionAccessGuard {

    private final ColegioRepository colegioRepository;
    private final SuscripcionColegioRepository suscripcionColegioRepository;
    private final Clock clock;

    public SchoolSubscriptionAccessGuard(
            ColegioRepository colegioRepository,
            SuscripcionColegioRepository suscripcionColegioRepository,
            Clock clock
    ) {
        this.colegioRepository = colegioRepository;
        this.suscripcionColegioRepository = suscripcionColegioRepository;
        this.clock = clock;
    }

    public void requireActiveSchoolSubscription(UUID colegioId) {
        requireExistingSchool(colegioId);
        if (!hasInstitutionalAccess(colegioId)) {
            throw new AccessDeniedException(
                    "El colegio no tiene una suscripcion activa");
        }
    }

    private void requireExistingSchool(UUID colegioId) {
        if (!colegioRepository.existsActiveById(colegioId)) {
            throw new SchoolNotFoundException(colegioId);
        }
    }

    private boolean hasInstitutionalAccess(UUID colegioId) {
        return suscripcionColegioRepository.findActiveByColegioId(colegioId)
                .map(this::canUseApp)
                .orElse(false);
    }

    private boolean canUseApp(SuscripcionColegio subscription) {
        return subscription.canUseAppAt(now());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
