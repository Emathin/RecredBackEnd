package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetGradesBySchoolResponse;
import com.recred.backend.application.GradeDetailsResponse;
import com.recred.backend.domain.models.Grado;

import java.util.UUID;

public record GradeResponseDTO(
        UUID id,
        UUID colegioId,
        UUID nivelId,
        String nivelDescripcion,
        String nombre,
        String anio,
        String division,
        Boolean activo
) {
    public static GradeResponseDTO fromDomain(Grado grade) {
        UUID colegioId = grade.getColegio() != null ? grade.getColegio().getId() : null;

        return new GradeResponseDTO(
                grade.getId(),
                colegioId,
                grade.getNivel() != null ? grade.getNivel().getId() : null,
                grade.getNivel() != null ? grade.getNivel().getDescripcion() : null,
                name(grade.getAnio(), grade.getDivision()),
                grade.getAnio(),
                grade.getDivision(),
                grade.getActivo() != null ? grade.getActivo() : true
        );
    }

    public static GradeResponseDTO fromApplication(GetGradesBySchoolResponse.Grade grade) {
        return new GradeResponseDTO(
                grade.id(),
                grade.colegioId(),
                grade.nivelId(),
                grade.nivelDescripcion(),
                name(grade.anio(), grade.division()),
                grade.anio(),
                grade.division(),
                grade.activo()
        );
    }

    public static GradeResponseDTO fromApplication(GradeDetailsResponse grade) {
        return new GradeResponseDTO(
                grade.id(),
                grade.colegioId(),
                grade.nivelId(),
                grade.nivelDescripcion(),
                name(grade.anio(), grade.division()),
                grade.anio(),
                grade.division(),
                grade.activo()
        );
    }

    private static String name(String anio, String division) {
        return anio + " " + division;
    }
}
