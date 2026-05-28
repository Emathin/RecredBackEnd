package com.recred.backend.application;

import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;

import java.util.List;
import java.util.UUID;

public class GetTimeSlotsBySchool {

    private final FranjaHorariaRepository repository;

    public GetTimeSlotsBySchool(FranjaHorariaRepository repository) {
        this.repository = repository;
    }

    public List<FranjaHoraria> execute(UUID colegioId) {
        return repository.findAllByColegioId(colegioId);
    }
}