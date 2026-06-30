package com.recred.backend.application;

import com.recred.backend.domain.models.Colegio;

import java.util.List;
import java.util.UUID;

public record GetActiveSchoolsResponse(
        List<School> schools
) {

    public static GetActiveSchoolsResponse fromDomain(List<Colegio> schools) {
        return new GetActiveSchoolsResponse(
                schools.stream()
                        .map(School::fromDomain)
                        .toList()
        );
    }

    public record School(
            UUID id,
            String nombre
    ) {

        private static School fromDomain(Colegio school) {
            return new School(
                    school.getId(),
                    school.getNombre()
            );
        }
    }
}
