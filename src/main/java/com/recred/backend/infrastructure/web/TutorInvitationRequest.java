package com.recred.backend.infrastructure.web;

public record TutorInvitationRequest(
        String email,
        String firstName,
        String lastName,
        String phone
) {
}
