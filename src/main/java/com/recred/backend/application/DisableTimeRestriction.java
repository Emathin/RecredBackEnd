package com.recred.backend.application;

import com.recred.backend.domain.models.TimeRestriction;
import com.recred.backend.domain.repositories.TimeRestrictionRepository;

import java.util.UUID;

public class DisableTimeRestriction {

    private final TimeRestrictionRepository repository;

    public DisableTimeRestriction(TimeRestrictionRepository repository) {
        this.repository = repository;
    }

    public void execute(UUID restrictionId) {
        TimeRestriction restriction = repository.findById(restrictionId)
                .orElseThrow(() -> new RuntimeException("Time restriction not found"));

        restriction.setActive(false);
        repository.save(restriction);
    }
}