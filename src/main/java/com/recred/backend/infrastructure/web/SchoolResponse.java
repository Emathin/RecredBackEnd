package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Colegio;

import java.util.UUID;

public record SchoolResponse(
        UUID id,
        String nombre
) {
    public static SchoolResponse fromDomain(Colegio school) {
        return new SchoolResponse(
                school.getId(),
                school.getNombre()
        );
    }
}
