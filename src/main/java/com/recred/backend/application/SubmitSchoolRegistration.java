package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationStatus;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.SchoolRegistrationRequestRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;

import java.time.LocalDateTime;

public class SubmitSchoolRegistration {

    private final SchoolRegistrationRequestRepository registrationRepository;
    private final ColegioRepository colegioRepository;
    private final UsuarioRepository usuarioRepository;

    public SubmitSchoolRegistration(
            SchoolRegistrationRequestRepository registrationRepository,
            ColegioRepository colegioRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.registrationRepository = registrationRepository;
        this.colegioRepository = colegioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public SubmitSchoolRegistrationResponse execute(
            SubmitSchoolRegistrationRequest registrationRequest
    ) {
        ensureSchoolRegistrationCanBeSubmitted(registrationRequest);
        SchoolRegistrationRequest registration = registrationFrom(registrationRequest);
        registrationRepository.save(registration);
        return new SubmitSchoolRegistrationResponse();
    }

    private void ensureSchoolRegistrationCanBeSubmitted(
            SubmitSchoolRegistrationRequest registrationRequest
    ) {
        ensureSchoolEmailIsAvailable(registrationRequest.schoolEmail());
        ensureSchoolCueIsAvailable(registrationRequest.schoolCue());
        ensureDirectorEmailIsAvailable(registrationRequest.directorEmail());
        ensureNoPendingRegistrationForSchoolEmail(registrationRequest.schoolEmail());
        ensureNoPendingRegistrationForSchoolCue(registrationRequest.schoolCue());
        ensureNoPendingRegistrationForDirector(registrationRequest.directorEmail());
    }

    private void ensureSchoolEmailIsAvailable(String schoolEmail) {
        if (colegioRepository.existsByEmail(schoolEmail)) {
            throw new IllegalArgumentException("Ya existe un colegio con ese email");
        }
    }

    private void ensureSchoolCueIsAvailable(String schoolCue) {
        if (colegioRepository.existsByCue(schoolCue)) {
            throw new IllegalArgumentException("Ya existe un colegio con ese CUE");
        }
    }

    private void ensureDirectorEmailIsAvailable(String directorEmail) {
        if (usuarioRepository.findByEmail(directorEmail).isPresent()) {
            throw new IllegalArgumentException("El email del directivo ya esta en uso");
        }
    }

    private void ensureNoPendingRegistrationForSchoolEmail(String schoolEmail) {
        if (registrationRepository.existsBySchoolEmailAndStatus(schoolEmail,
                SchoolRegistrationStatus.PENDING)) {
            throw new IllegalArgumentException("Ya hay una solicitud pendiente para ese colegio");
        }
    }

    private void ensureNoPendingRegistrationForSchoolCue(String schoolCue) {
        if (registrationRepository.existsBySchoolCueAndStatus(schoolCue,
                SchoolRegistrationStatus.PENDING)) {
            throw new IllegalArgumentException("Ya hay una solicitud pendiente con ese CUE");
        }
    }

    private void ensureNoPendingRegistrationForDirector(String directorEmail) {
        if (registrationRepository.existsByDirectorEmailAndStatus(directorEmail,
                SchoolRegistrationStatus.PENDING)) {
            throw new IllegalArgumentException("Ya hay una solicitud pendiente para ese directivo");
        }
    }

    private SchoolRegistrationRequest registrationFrom(SubmitSchoolRegistrationRequest request) {
        SchoolRegistrationRequest entity = new SchoolRegistrationRequest();
        entity.setSchoolName(request.schoolName());
        entity.setSchoolEmail(request.schoolEmail());
        entity.setSchoolPhone(request.schoolPhone());
        entity.setSchoolCue(request.schoolCue());
        setDirectorFields(entity, request);
        setSubmissionTimestamps(entity);
        return entity;
    }

    private void setDirectorFields(
            SchoolRegistrationRequest entity,
            SubmitSchoolRegistrationRequest request
    ) {
        entity.setDirectorFirstName(request.directorFirstName());
        entity.setDirectorLastName(request.directorLastName());
        entity.setDirectorEmail(request.directorEmail());
        entity.setDirectorPhone(request.directorPhone());
        entity.setDirectorDni(request.directorDni());
        entity.setDirectorUsername(request.directorUsername());
    }

    private void setSubmissionTimestamps(SchoolRegistrationRequest entity) {
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
    }
}
