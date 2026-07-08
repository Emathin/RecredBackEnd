package com.recred.backend.application;

import java.util.UUID;

public record GetChildrenByTutorRequest(
        UUID tutorId,
        String cognitoSubject
) {
    public static GetChildrenByTutorRequest byTutorId(UUID tutorId) {
        return new GetChildrenByTutorRequest(tutorId, null);
    }

    public static GetChildrenByTutorRequest byCognitoSubject(String cognitoSubject) {
        return new GetChildrenByTutorRequest(null, cognitoSubject);
    }
}
