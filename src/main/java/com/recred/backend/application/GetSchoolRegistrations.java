package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolRegistrationStatus;
import com.recred.backend.domain.repositories.SchoolRegistrationRequestRepository;

import java.util.List;
import java.util.stream.Collectors;

public class GetSchoolRegistrations {

    private final SchoolRegistrationRequestRepository repository;

    public GetSchoolRegistrations(SchoolRegistrationRequestRepository repository) {
        this.repository = repository;
    }

    public List<GetSchoolRegistrationsResponse> execute(String statusStr) {
        SchoolRegistrationStatus status = SchoolRegistrationStatus.valueOf(statusStr);
        return repository.findByStatus(status).stream()
                .map(GetSchoolRegistrationsResponse::fromDomain)
                .collect(Collectors.toList());
    }
}
