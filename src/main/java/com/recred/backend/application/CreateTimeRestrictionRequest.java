package com.recred.backend.application;
import java.util.UUID;
public record CreateTimeRestrictionRequest(
        UUID studentId,
        UUID timeSlotId,
        UUID categoryId,
        UUID classificationId,
        String cognitoSub
) {}
