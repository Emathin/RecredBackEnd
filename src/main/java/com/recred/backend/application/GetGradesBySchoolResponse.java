package com.recred.backend.application;

import com.recred.backend.domain.models.Grado;

import java.util.List;
import java.util.UUID;

public record GetGradesBySchoolResponse(
        List<Grade> grades
) {

    public static GetGradesBySchoolResponse fromDomain(List<Grado> grades) {
        return new GetGradesBySchoolResponse(
                grades.stream()
                        .map(Grade::fromDomain)
                        .toList()
        );
    }

    public record Grade(
            UUID id,
            String anio,
            String division
    ) {

        private static Grade fromDomain(Grado grade) {
            return new Grade(
                    grade.getId(),
                    grade.getAnio(),
                    grade.getDivision()
            );
        }
    }
}
