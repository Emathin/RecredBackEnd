package com.recred.backend.application;

import java.util.UUID;

public record GetTutorHistoryRequest(
        UUID tutorId,
        String cognitoSubject
) {

    public static GetTutorHistoryRequest byTutorId(UUID tutorId) {
        return new GetTutorHistoryRequest(tutorId, null);
    }

    public static GetTutorHistoryRequest byCognitoSubject(String cognitoSubject) {
        return new GetTutorHistoryRequest(null, cognitoSubject);
    }
}
