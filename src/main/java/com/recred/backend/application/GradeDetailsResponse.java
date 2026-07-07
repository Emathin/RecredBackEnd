package com.recred.backend.application;

import com.recred.backend.domain.models.Grado;

import java.util.UUID;

public record GradeDetailsResponse(
        UUID id,
        UUID colegioId,
        UUID nivelId,
        String nivelDescripcion,
        String anio,
        String division,
        Boolean activo
) {

    public static GradeDetailsResponse fromDomain(Grado grade) {
        UUID colegioId = grade.getColegio() != null ? grade.getColegio().getId() : null;
        UUID nivelId = grade.getNivel() != null ? grade.getNivel().getId() : null;
        String nivelDescripcion = grade.getNivel() != null
                ? grade.getNivel().getDescripcion()
                : null;
        Boolean activo = grade.getActivo() != null ? grade.getActivo() : true;

        return new GradeDetailsResponse(
                grade.getId(),
                colegioId,
                nivelId,
                nivelDescripcion,
                grade.getAnio(),
                grade.getDivision(),
                activo
        );
    }
}
