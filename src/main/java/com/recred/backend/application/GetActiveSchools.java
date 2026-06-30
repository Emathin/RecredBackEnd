package com.recred.backend.application;

import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.repositories.ColegioRepository;

import java.util.List;

public class GetActiveSchools {

    private final ColegioRepository schoolRepository;

    public GetActiveSchools(ColegioRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    public GetActiveSchoolsResponse execute() {
        List<Colegio> schools = findActiveSchools();

        return toResponse(schools);
    }

    private List<Colegio> findActiveSchools() {
        return schoolRepository.findAllActive();
    }

    private GetActiveSchoolsResponse toResponse(List<Colegio> schools) {
        return GetActiveSchoolsResponse.fromDomain(schools);
    }
}
