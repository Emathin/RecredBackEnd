package com.recred.backend.application;

import com.recred.backend.domain.models.TimeBadge;
import com.recred.backend.domain.repositories.TimeBadgeRepository;

import java.util.List;
import java.util.UUID;

public class GetTimeSlotsBySchool {

    private final TimeBadgeRepository repository;

    public GetTimeSlotsBySchool(TimeBadgeRepository repository) {
        this.repository = repository;
    }

    public List<TimeBadge> execute(UUID colegioId) {
        return repository.findAllByColegioId(colegioId);
    }
}