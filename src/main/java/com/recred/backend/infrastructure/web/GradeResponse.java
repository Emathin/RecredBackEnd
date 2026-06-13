package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Grado;

import java.util.UUID;

public record GradeResponse(
        UUID id,
        String year,
        String division,
        String description
) {
    public static GradeResponse fromDomain(Grado grade) {
        return new GradeResponse(
                grade.getId(),
                grade.getAnio(),
                grade.getDivision(),
                grade.getAnio() + " " + grade.getDivision()
        );
    }
}
