package com.recred.backend.domain.services;

public interface CognitoTutorIdentityService {
    CreatedCognitoUser createTutorUser(String username, String email,
                                       String firstName, String lastName);
}
