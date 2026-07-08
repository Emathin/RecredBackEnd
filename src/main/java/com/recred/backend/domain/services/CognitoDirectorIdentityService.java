package com.recred.backend.domain.services;

public interface CognitoDirectorIdentityService {
    CreatedCognitoUser createDirectorUser(String username, String email, 
                                          String firstName, String lastName);
}
