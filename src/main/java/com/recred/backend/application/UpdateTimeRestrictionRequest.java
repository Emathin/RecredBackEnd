package com.recred.backend.application;

import java.util.UUID;

public record UpdateTimeRestrictionRequest(
        UUID restrictionId,
        UUID timeSlotId,
        UUID categoryId,
        UUID classificationId
) {}
