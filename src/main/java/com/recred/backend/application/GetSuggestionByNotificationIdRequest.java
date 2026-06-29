package com.recred.backend.application;

import java.util.UUID;

public record GetSuggestionByNotificationIdRequest(
        UUID notificationId,
        String cognitoSubject
) {
}
