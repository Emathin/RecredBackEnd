package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.services.CognitoUserProfile;

public record SyncUserRequest(
        String email,
        String nombre,
        String apellido,
        String firstName,
        String lastName
) {

    CognitoUserProfile toProfile() {
        return new CognitoUserProfile(
                email,
                firstNonBlank(nombre, firstName),
                firstNonBlank(apellido, lastName)
        );
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }
}
