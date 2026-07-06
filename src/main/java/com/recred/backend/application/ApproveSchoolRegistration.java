package com.recred.backend.application;

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

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

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
    public ApproveSchoolRegistrationResponse execute(ApproveSchoolRegistrationRequest request) {
        SchoolRegistrationRequest registration = fetchPendingRegistration(request.registrationId());
        Colegio newColegio = createAndSaveColegio(registration);
        createAndSaveDirectorUser(registration, newColegio);
        updateRegistrationStatus(registration);
        return new ApproveSchoolRegistrationResponse();
    }

    private SchoolRegistrationRequest fetchPendingRegistration(UUID id) {
        SchoolRegistrationRequest registration = registrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found"));
        if (registration.getStatus() != SchoolRegistrationStatus.PENDING) {
            throw new IllegalStateException("Registration is not pending");
        }
        return registration;
    }

    private Colegio createAndSaveColegio(SchoolRegistrationRequest req) {
        Colegio colegio = new Colegio();
        colegio.setNombre(req.getSchoolName());
        colegio.setEmail(req.getSchoolEmail());
        colegio.setTelefono(req.getSchoolPhone());
        colegio.setCue(req.getSchoolCue());
        colegio.setActivo(true);
        colegio.setCreadoEn(LocalDateTime.now());
        colegio.setActualizadoEn(LocalDateTime.now());
        return colegioRepository.save(colegio);
    }

    private void createAndSaveDirectorUser(SchoolRegistrationRequest req, Colegio colegio) {
        CreatedCognitoUser cognitoUser;
        try {
            cognitoUser = cognitoService.createDirectorUser(
                    req.getDirectorUsername(), 
                    req.getDirectorEmail(), 
                    req.getDirectorFirstName(), 
                    req.getDirectorLastName());
        } catch (com.recred.backend.domain.exceptions.UsernameExistsException e) {
            String suffix = UUID.randomUUID().toString().substring(0, 4);
            cognitoUser = cognitoService.createDirectorUser(
                    req.getDirectorUsername() + "-" + suffix, 
                    req.getDirectorEmail(), 
                    req.getDirectorFirstName(), 
                    req.getDirectorLastName());
        }
        Usuario usuario = buildUsuario(req, colegio, cognitoUser.sub());
        usuarioRepository.save(usuario);
    }

    private Usuario buildUsuario(SchoolRegistrationRequest req, Colegio col, String idCognito) {
        Usuario usuario = new Usuario();
        usuario.setColegio(col);
        usuario.setIdCognito(idCognito);
        usuario.setNombre(req.getDirectorFirstName());
        usuario.setApellido(req.getDirectorLastName());
        usuario.setEmail(req.getDirectorEmail());
        usuario.setTelefono(req.getDirectorPhone());
        usuario.setDni(req.getDirectorDni());
        usuario.setRol(RolUsuario.DIRECTIVO_COLEGIO);
        usuario.setCreadoEn(LocalDateTime.now());
        usuario.setActualizadoEn(LocalDateTime.now());
        return usuario;
    }

    private void updateRegistrationStatus(SchoolRegistrationRequest registration) {
        registration.setStatus(SchoolRegistrationStatus.APPROVED);
        registration.setUpdatedAt(LocalDateTime.now());
        registrationRepository.save(registration);
    }
}
