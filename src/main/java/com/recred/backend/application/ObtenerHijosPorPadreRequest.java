package com.recred.backend.application;

import java.util.UUID;

public record ObtenerHijosPorPadreRequest(
        UUID tutorId,
        String cognitoSubject
) {
    public static ObtenerHijosPorPadreRequest byTutorId(UUID tutorId) {
        return new ObtenerHijosPorPadreRequest(tutorId, null);
    }

    public static ObtenerHijosPorPadreRequest byCognitoSubject(String cognitoSubject) {
        return new ObtenerHijosPorPadreRequest(null, cognitoSubject);
    }
}
