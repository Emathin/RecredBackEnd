package com.recred.backend.application;
import java.util.UUID;
public record CreateTimeRestrictionResponse(
        UUID id,
        UUID studentId,
        UUID timeSlotId,
        UUID categoryId,
        UUID classificationId,
        boolean activa,
        UUID creadoPorUsuarioId
) {}
