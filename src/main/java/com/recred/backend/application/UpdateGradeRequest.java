package com.recred.backend.application;

import java.util.UUID;

public record UpdateGradeRequest(
        UUID colegioId,
        UUID gradeId,
        UUID nivelId,
        String anio,
        String division
) {
}
