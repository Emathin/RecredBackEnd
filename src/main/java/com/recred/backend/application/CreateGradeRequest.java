package com.recred.backend.application;

import java.util.UUID;

public record CreateGradeRequest(
        UUID colegioId,
        UUID nivelId,
        String anio,
        String division
) {
}
