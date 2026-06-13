package com.recred.backend.application;

import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.repositories.ColegioRepository;

import java.util.List;

public class GetActiveSchools {

    private final ColegioRepository schoolRepository;

    public GetActiveSchools(ColegioRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    public List<Colegio> execute() {
        return schoolRepository.findAllActive();
    }
}
