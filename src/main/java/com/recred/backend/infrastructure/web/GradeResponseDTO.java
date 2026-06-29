package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetGradesBySchoolResponse;
import com.recred.backend.domain.models.Grado;

import java.util.UUID;

public record GradeResponseDTO(
        UUID id,
        String nombre
) {
    public static GradeResponseDTO fromDomain(Grado grade) {
        return new GradeResponseDTO(
                grade.getId(),
                grade.getAnio() + " " + grade.getDivision()
        );
    }

    public static GradeResponseDTO fromApplication(GetGradesBySchoolResponse.Grade grade) {
        return new GradeResponseDTO(
                grade.id(),
                grade.anio() + " " + grade.division()
        );
    }
}
