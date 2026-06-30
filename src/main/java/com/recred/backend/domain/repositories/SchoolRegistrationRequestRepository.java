package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.SchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolRegistrationRequestRepository {
    void save(SchoolRegistrationRequest request);
    Optional<SchoolRegistrationRequest> findById(UUID id);
    List<SchoolRegistrationRequest> findByStatus(SchoolRegistrationStatus status);
}
