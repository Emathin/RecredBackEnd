package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationStatus;
import com.recred.backend.domain.repositories.SchoolRegistrationRequestRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public class RejectSchoolRegistration {

    private final SchoolRegistrationRequestRepository registrationRepository;

    public RejectSchoolRegistration(
            SchoolRegistrationRequestRepository registrationRepository
    ) {
        this.registrationRepository = registrationRepository;
    }

    public RejectSchoolRegistrationResponse execute(
            RejectSchoolRegistrationRequest rejectionRequest
    ) {
        SchoolRegistrationRequest registration =
                findPendingRegistration(rejectionRequest.registrationId());
        rejectRegistration(registration);
        return new RejectSchoolRegistrationResponse();
    }

    private SchoolRegistrationRequest findPendingRegistration(UUID registrationId) {
        SchoolRegistrationRequest registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found"));
        if (registrationIsNotPending(registration)) {
            throw new IllegalStateException("Registration is not pending");
        }
        return registration;
    }

    private boolean registrationIsNotPending(SchoolRegistrationRequest registration) {
        return registration.getStatus() != SchoolRegistrationStatus.PENDING;
    }

    private void rejectRegistration(SchoolRegistrationRequest registration) {
        registration.setStatus(SchoolRegistrationStatus.REJECTED);
        registration.setUpdatedAt(LocalDateTime.now());
        registrationRepository.save(registration);
    }
}
