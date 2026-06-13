package com.recred.backend.application;

import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;

import java.util.List;
import java.util.UUID;

public class GetTimeRestrictionsByStudent {

    private final RestriccionHorariaRepository repository;

    public GetTimeRestrictionsByStudent(RestriccionHorariaRepository repository) {
        this.repository = repository;
    }

    public List<RestriccionHoraria> execute(UUID studentId) {
        return repository.findAllByAlumnoEntityId(studentId);
    }
}