package com.recred.backend.domain.services;

public interface CognitoStudentIdentityService {
    CreatedCognitoUser createStudentUser(String username, String email,
                                         String nombre, String apellido);
}
