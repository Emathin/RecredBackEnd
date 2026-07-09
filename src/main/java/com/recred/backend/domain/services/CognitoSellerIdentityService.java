package com.recred.backend.domain.services;

public interface CognitoSellerIdentityService {
    CreatedCognitoUser createSellerUser(String username, String email,
                                        String firstName, String lastName);

    void resendTemporaryPassword(String username);

    void disableUser(String username);
}
