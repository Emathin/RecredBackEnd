package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationStatus;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.SchoolRegistrationRequestRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;

import java.time.LocalDateTime;

public class SubmitSchoolRegistration {

    private final SchoolRegistrationRequestRepository repository;
    private final ColegioRepository colegioRepository;
    private final UsuarioRepository usuarioRepository;

    public SubmitSchoolRegistration(SchoolRegistrationRequestRepository repository,
                                    ColegioRepository colegioRepository,
                                    UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.colegioRepository = colegioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public SubmitSchoolRegistrationResponse execute(SubmitSchoolRegistrationRequest request) {
        validateRequest(request);
        SchoolRegistrationRequest newRequest = buildRequestEntity(request);
        repository.save(newRequest);
        return new SubmitSchoolRegistrationResponse();
    }

    private void validateRequest(SubmitSchoolRegistrationRequest req) {
        if (colegioRepository.existsByEmail(req.schoolEmail())) {
            throw new IllegalArgumentException("Ya existe un colegio con ese email");
        }
        if (colegioRepository.existsByCue(req.schoolCue())) {
            throw new IllegalArgumentException("Ya existe un colegio con ese CUE");
        }
        if (usuarioRepository.findByEmail(req.directorEmail()).isPresent()) {
            throw new IllegalArgumentException("El email del directivo ya esta en uso");
        }
        if (repository.existsBySchoolEmailAndStatus(req.schoolEmail(), 
                SchoolRegistrationStatus.PENDING)) {
            throw new IllegalArgumentException("Ya hay una solicitud pendiente para ese colegio");
        }
        if (repository.existsBySchoolCueAndStatus(req.schoolCue(), 
                SchoolRegistrationStatus.PENDING)) {
            throw new IllegalArgumentException("Ya hay una solicitud pendiente con ese CUE");
        }
        if (repository.existsByDirectorEmailAndStatus(req.directorEmail(), 
                SchoolRegistrationStatus.PENDING)) {
            throw new IllegalArgumentException("Ya hay una solicitud pendiente para ese directivo");
        }
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
