package com.recred.backend.application;

import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;

import java.util.UUID;

public class DisableTimeRestriction {

    private final RestriccionHorariaRepository repository;

    public DisableTimeRestriction(RestriccionHorariaRepository repository) {
        this.repository = repository;
    }

    public void execute(UUID restrictionId) {
        RestriccionHoraria restriction = repository.findById(restrictionId)
                .orElseThrow(() -> new RuntimeException("Time restriction not found"));

        restriction.setActiva(false);
        repository.save(restriction);
    }
}