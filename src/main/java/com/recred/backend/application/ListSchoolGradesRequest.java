package com.recred.backend.application;

import java.util.UUID;

public record ListSchoolGradesRequest(
        UUID colegioId,
        boolean includeInactive
) {
}
