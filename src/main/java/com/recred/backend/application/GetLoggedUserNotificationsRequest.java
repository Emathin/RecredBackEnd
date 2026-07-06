package com.recred.backend.application;

public record GetLoggedUserNotificationsRequest(
        String cognitoSubject,
        boolean unreadOnly,
        int page,
        int size
) {
}
