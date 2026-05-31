package com.recred.backend.application;

import com.recred.backend.domain.models.TimeRestriction;
import com.recred.backend.domain.repositories.TimeRestrictionRepository;

import java.util.List;
import java.util.UUID;

public class GetTimeRestrictionsByStudent {

    private final TimeRestrictionRepository repository;

    public GetTimeRestrictionsByStudent(TimeRestrictionRepository repository) {
        this.repository = repository;
    }

    public List<TimeRestriction> execute(UUID studentId) {
        return repository.findAllByAlumnoId(studentId);
    }
}