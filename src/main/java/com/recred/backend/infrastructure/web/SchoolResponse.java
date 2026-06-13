package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Colegio;

import java.util.UUID;

public record SchoolResponse(
        UUID id,
        String name,
        String cue,
        String level
) {
    public static SchoolResponse fromDomain(Colegio school) {
        String level = school.getNivel() != null
                ? school.getNivel().getDescripcion()
                : null;

        return new SchoolResponse(
                school.getId(),
                school.getNombre(),
                school.getCue(),
                level
        );
    }
}
