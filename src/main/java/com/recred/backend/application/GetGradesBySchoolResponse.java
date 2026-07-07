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
            UUID colegioId,
            UUID nivelId,
            String nivelDescripcion,
            String anio,
            String division,
            Boolean activo
    ) {

        private static Grade fromDomain(Grado grade) {
            UUID colegioId = grade.getColegio() != null
                    ? grade.getColegio().getId()
                    : null;
            UUID nivelId = grade.getNivel() != null
                    ? grade.getNivel().getId()
                    : null;
            String nivelDescripcion = grade.getNivel() != null
                    ? grade.getNivel().getDescripcion()
                    : null;

            return new Grade(
                    grade.getId(),
                    colegioId,
                    nivelId,
                    nivelDescripcion,
                    grade.getAnio(),
                    grade.getDivision(),
                    grade.getActivo() != null ? grade.getActivo() : true
            );
        }
    }
}
