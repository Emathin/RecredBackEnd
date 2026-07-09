package com.recred.backend.application;

public record PrepareTutorInvitationAccountRequest(
        String token,
        String username
) {
}
