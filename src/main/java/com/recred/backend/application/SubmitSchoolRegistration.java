package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolRegistrationRequest;
import com.recred.backend.domain.repositories.SchoolRegistrationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubmitSchoolRegistration {

    private final SchoolRegistrationRequestRepository repository;

    public void execute(SubmitSchoolRegistrationRequest request) {
        SchoolRegistrationRequest newRequest = buildRequestEntity(request);
        repository.save(newRequest);
    }

    private SchoolRegistrationRequest buildRequestEntity(SubmitSchoolRegistrationRequest req) {
        SchoolRegistrationRequest entity = new SchoolRegistrationRequest();
        entity.setSchoolName(req.schoolName());
        entity.setSchoolEmail(req.schoolEmail());
        entity.setSchoolPhone(req.schoolPhone());
        entity.setSchoolCue(req.schoolCue());
        entity.setSchoolLevel(req.schoolLevel());
        setDirectorFields(entity, req);
        setMetadata(entity);
        return entity;
    }

    private void setDirectorFields(SchoolRegistrationRequest entity, 
                                   SubmitSchoolRegistrationRequest req) {
        entity.setDirectorFirstName(req.directorFirstName());
        entity.setDirectorLastName(req.directorLastName());
        entity.setDirectorEmail(req.directorEmail());
        entity.setDirectorPhone(req.directorPhone());
        entity.setDirectorDni(req.directorDni());
        entity.setDirectorUsername(req.directorUsername());
    }

    private void setMetadata(SchoolRegistrationRequest entity) {
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
    }
}
