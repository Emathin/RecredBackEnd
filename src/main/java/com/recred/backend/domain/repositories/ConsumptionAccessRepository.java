package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.ConsumptionRequester;
import java.util.Optional;
import java.util.UUID;

public interface ConsumptionAccessRepository {
    Optional<ConsumptionRequester> findRequesterByCognitoSubject(String cognitoSubject);

    boolean tutorCanAccessStudent(UUID userId, UUID studentId);
}
