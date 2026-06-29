package com.recred.backend.application;

import java.util.UUID;

public record GetPurchaseStatusRequest(
        String cognitoSubject,
        UUID purchaseId
) {
}
