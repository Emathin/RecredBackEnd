package com.recred.backend.infrastructure.web;

public record UpdateUserProfileRequest(
        String firstName,
        String lastName,
        String phone,
        String documentNumber
) {
}
