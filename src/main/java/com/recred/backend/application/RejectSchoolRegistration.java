package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationStatus;
import com.recred.backend.domain.repositories.SchoolRegistrationRequestRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public class RejectSchoolRegistration {

    private final SchoolRegistrationRequestRepository repository;

    public RejectSchoolRegistration(SchoolRegistrationRequestRepository repository) {
        this.repository = repository;
    }

    public RejectSchoolRegistrationResponse execute(RejectSchoolRegistrationRequest request) {
        SchoolRegistrationRequest registration = fetchPendingRegistration(request.registrationId());
        updateRegistrationStatus(registration);
        return new RejectSchoolRegistrationResponse();
    }

    private SchoolRegistrationRequest fetchPendingRegistration(UUID id) {
        SchoolRegistrationRequest registration = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found"));
        if (registration.getStatus() != SchoolRegistrationStatus.PENDING) {
            throw new IllegalStateException("Registration is not pending");
        }
        return registration;
    }

    private void updateRegistrationStatus(SchoolRegistrationRequest registration) {
        registration.setStatus(SchoolRegistrationStatus.REJECTED);
        registration.setUpdatedAt(LocalDateTime.now());
        repository.save(registration);
    }
}
