package com.recred.backend.infrastructure.web;

import java.util.UUID;

public record GradeRequestDTO(
        UUID nivelId,
        String anio,
        String division
) {
}
