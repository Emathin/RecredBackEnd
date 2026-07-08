package com.recred.backend.application;

import com.recred.backend.domain.exceptions.UsernameExistsException;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.SchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationStatus;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.SchoolRegistrationRequestRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.CognitoDirectorIdentityService;
import com.recred.backend.domain.services.CreatedCognitoUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

public class ApproveSchoolRegistration {

    private final SchoolRegistrationRequestRepository registrationRepository;
    private final ColegioRepository colegioRepository;
    private final UsuarioRepository usuarioRepository;
    private final CognitoDirectorIdentityService cognitoService;

    public ApproveSchoolRegistration(SchoolRegistrationRequestRepository registrationRepository,
                                     ColegioRepository colegioRepository,
                                     UsuarioRepository usuarioRepository,
                                     CognitoDirectorIdentityService cognitoService) {
        this.registrationRepository = registrationRepository;
        this.colegioRepository = colegioRepository;
        this.usuarioRepository = usuarioRepository;
        this.cognitoService = cognitoService;
    }

    @Transactional
    public ApproveSchoolRegistrationResponse execute(
            ApproveSchoolRegistrationRequest approvalRequest) {
        SchoolRegistrationRequest registration =
                findPendingRegistration(approvalRequest.registrationId());
        Colegio school = createAndSaveSchool(registration);
        createAndSaveDirectorUser(registration, school);
        approveRegistration(registration);
        return new ApproveSchoolRegistrationResponse();
    }

    private SchoolRegistrationRequest findPendingRegistration(UUID registrationId) {
        SchoolRegistrationRequest registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found"));
        if (registration.getStatus() != SchoolRegistrationStatus.PENDING) {
            throw new IllegalStateException("Registration is not pending");
        }
        return registration;
    }

    private Colegio createAndSaveSchool(SchoolRegistrationRequest registration) {
        LocalDateTime now = LocalDateTime.now();
        Colegio colegio = new Colegio();
        colegio.setNombre(registration.getSchoolName());
        colegio.setEmail(registration.getSchoolEmail());
        colegio.setTelefono(registration.getSchoolPhone());
        colegio.setCue(registration.getSchoolCue());
        colegio.setActivo(true);
        colegio.setCreadoEn(now);
        colegio.setActualizadoEn(now);
        return colegioRepository.save(colegio);
    }

    private void createAndSaveDirectorUser(SchoolRegistrationRequest registration, Colegio school) {
        CreatedCognitoUser cognitoUser = createDirectorCognitoUser(registration);
        Usuario usuario = buildDirectorUser(registration, school, cognitoUser.sub());
        usuarioRepository.save(usuario);
    }

    private CreatedCognitoUser createDirectorCognitoUser(
            SchoolRegistrationRequest registration) {
        try {
            return createDirectorCognitoUser(registration, registration.getDirectorUsername());
        } catch (UsernameExistsException e) {
            return createDirectorCognitoUser(registration, usernameWithRandomSuffix(
                    registration.getDirectorUsername()));
        }
    }

    private CreatedCognitoUser createDirectorCognitoUser(
            SchoolRegistrationRequest registration,
            String username) {
        return cognitoService.createDirectorUser(
                username,
                registration.getDirectorEmail(),
                registration.getDirectorFirstName(),
                registration.getDirectorLastName());
    }

    private String usernameWithRandomSuffix(String username) {
        String suffix = UUID.randomUUID().toString().substring(0, 4);
        return username + "-" + suffix;
    }

    private Usuario buildDirectorUser(
            SchoolRegistrationRequest registration,
            Colegio school,
            String cognitoSubject) {
        LocalDateTime now = LocalDateTime.now();
        Usuario usuario = new Usuario();
        usuario.setColegio(school);
        usuario.setIdCognito(cognitoSubject);
        usuario.setNombre(registration.getDirectorFirstName());
        usuario.setApellido(registration.getDirectorLastName());
        usuario.setEmail(registration.getDirectorEmail());
        usuario.setTelefono(registration.getDirectorPhone());
        usuario.setDni(registration.getDirectorDni());
        usuario.setRol(RolUsuario.DIRECTIVO_COLEGIO);
        usuario.setCreadoEn(now);
        usuario.setActualizadoEn(now);
        return usuario;
    }

    private void approveRegistration(SchoolRegistrationRequest registration) {
        registration.setStatus(SchoolRegistrationStatus.APPROVED);
        registration.setUpdatedAt(LocalDateTime.now());
        registrationRepository.save(registration);
    }
}
