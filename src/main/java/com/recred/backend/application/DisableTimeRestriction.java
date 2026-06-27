package com.recred.backend.application;

import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;

import java.util.UUID;

public class DisableTimeRestriction {

    private final RestriccionHorariaRepository repository;

    public DisableTimeRestriction(RestriccionHorariaRepository repository) {
        this.repository = repository;
    }

    public void execute(DisableTimeRestrictionRequest request) {
        RestriccionHoraria restriction = findRestriccionHorariaById(request.restrictionId());
        restriction.setActiva(false);
        repository.save(restriction);
    }

    private RestriccionHoraria findRestriccionHorariaById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Time restriction not found"));
    }
}