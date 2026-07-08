package com.recred.backend.application;

import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;

import java.util.UUID;

public class DisableTimeRestriction {

    private final RestriccionHorariaRepository repository;

    public DisableTimeRestriction(RestriccionHorariaRepository repository) {
        this.repository = repository;
    }

    public DisableTimeRestrictionResponse execute(
            DisableTimeRestrictionRequest disableRequest) {
        RestriccionHoraria restriction = findTimeRestriction(disableRequest.restrictionId());
        disableRestriction(restriction);
        return new DisableTimeRestrictionResponse();
    }

    private RestriccionHoraria findTimeRestriction(UUID restrictionId) {
        return repository.findById(restrictionId)
                .orElseThrow(() -> new RuntimeException("Time restriction not found"));
    }

    private void disableRestriction(RestriccionHoraria restriction) {
        restriction.disable();
        repository.save(restriction);
    }
}
