package com.recred.backend.application;

import java.util.UUID;

public record GetGradesBySchoolRequest(
        UUID schoolId
) {

    public void validate() {
        if (schoolId == null) {
            throw new IllegalArgumentException("El colegio es obligatorio");
        }
    }
}
