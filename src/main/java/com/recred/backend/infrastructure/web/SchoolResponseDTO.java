package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetActiveSchoolsResponse;
import com.recred.backend.domain.models.Colegio;

import java.util.UUID;

public record SchoolResponseDTO(
        UUID id,
        String nombre
) {
    public static SchoolResponseDTO fromDomain(Colegio school) {
        return new SchoolResponseDTO(
                school.getId(),
                school.getNombre()
        );
    }

    public static SchoolResponseDTO fromApplication(GetActiveSchoolsResponse.School school) {
        return new SchoolResponseDTO(
                school.id(),
                school.nombre()
        );
    }
}
