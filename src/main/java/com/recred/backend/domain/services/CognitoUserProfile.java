package com.recred.backend.domain.services;

public record CognitoUserProfile(String email, String nombre, String apellido) {

    public static CognitoUserProfile empty() {
        return new CognitoUserProfile(null, null, null);
    }
}
