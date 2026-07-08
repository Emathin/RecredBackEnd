package com.recred.backend.application;

import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.repositories.ColegioRepository;

import java.util.List;

public class GetActiveSchools {

    private final ColegioRepository schoolRepository;

    public GetActiveSchools(ColegioRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    public GetActiveSchoolsResponse execute(GetActiveSchoolsRequest activeSchoolsRequest) {
        List<Colegio> activeSchools = findActiveSchools();

        return responseFrom(activeSchools);
    }

    private List<Colegio> findActiveSchools() {
        return schoolRepository.findAllActive();
    }

    private GetActiveSchoolsResponse responseFrom(List<Colegio> schools) {
        return GetActiveSchoolsResponse.fromDomain(schools);
    }
}
