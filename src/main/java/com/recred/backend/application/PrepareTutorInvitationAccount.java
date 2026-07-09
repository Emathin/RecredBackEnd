package com.recred.backend.application;

import com.recred.backend.domain.exceptions.UsernameExistsException;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.TutorInvitationRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.CognitoTutorIdentityService;
import com.recred.backend.domain.services.CreatedCognitoUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

public class PrepareTutorInvitationAccount {

    public static final String LOGIN_REQUIRED = "LOGIN_REQUIRED";
    public static final String USERNAME_REQUIRED = "USERNAME_REQUIRED";
    public static final String ACCOUNT_CREATED_TEMPORARY_PASSWORD_SENT =
            "ACCOUNT_CREATED_TEMPORARY_PASSWORD_SENT";

    private final UsuarioRepository userRepository;
    private final TutorRepository tutorRepository;
    private final TutorInvitationRepository invitationRepository;
    private final CognitoTutorIdentityService cognitoService;
    private final TutorInvitationTokenService tokenService;
    private final Clock clock;

    public PrepareTutorInvitationAccount(
            UsuarioRepository userRepository,
            TutorRepository tutorRepository,
            TutorInvitationRepository invitationRepository,
            CognitoTutorIdentityService cognitoService,
            Clock clock) {
        this.userRepository = userRepository;
        this.tutorRepository = tutorRepository;
        this.invitationRepository = invitationRepository;
        this.cognitoService = cognitoService;
        this.tokenService = new TutorInvitationTokenService();
        this.clock = clock;
    }

    @Transactional
    public PrepareTutorInvitationAccountResponse execute(
            PrepareTutorInvitationAccountRequest prepareRequest) {
        TutorInvitation invitation = findInvitationByToken(prepareRequest.token());
        ensureInvitationCanPrepareAccount(invitation);

        String result = determineRegistrationResult(invitation, prepareRequest);

        return PrepareTutorInvitationAccountResponse.fromDomain(invitation, result);
    }

    private String determineRegistrationResult(
            TutorInvitation invitation, PrepareTutorInvitationAccountRequest prepareRequest) {
        if (isUserAlreadyRegistered(invitation)) {
            return LOGIN_REQUIRED;
        }
        if (isUsernameMissing(prepareRequest)) {
            return USERNAME_REQUIRED;
        }
        return registerTutorAccount(invitation, prepareRequest.username());
    }

    private boolean isUsernameMissing(PrepareTutorInvitationAccountRequest prepareRequest) {
        return isBlank(prepareRequest.username());
    }

    private boolean isUserAlreadyRegistered(TutorInvitation invitation) {
        return userRepository.findByEmail(normalizeEmail(invitation.getEmail())).isPresent();
    }

    private String registerTutorAccount(TutorInvitation invitation, String username) {
        try {
            Usuario user = createParentUser(invitation, username);
            createTutorIfMissing(user);
            return ACCOUNT_CREATED_TEMPORARY_PASSWORD_SENT;
        } catch (UsernameExistsException exception) {
            throw new IllegalArgumentException("El nombre de usuario ya esta en uso");
        }
    }

    private TutorInvitation findInvitationByToken(String token) {
        return invitationRepository.findByTokenHash(tokenService.hash(token))
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la invitacion"));
    }

    private void ensureInvitationCanPrepareAccount(TutorInvitation invitation) {
        if (invitation.getStatus() != TutorInvitationStatus.PENDING) {
            throw new IllegalArgumentException("La invitacion no esta pendiente");
        }
        if (invitation.getExpiresAt().isBefore(now())) {
            invitation.setStatus(TutorInvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new IllegalArgumentException("La invitacion esta vencida");
        }
    }

    private Usuario createParentUser(TutorInvitation invitation, String username) {
        CreatedCognitoUser cognitoUser = cognitoService.createTutorUser(
                normalizeUsername(username),
                normalizeEmail(invitation.getEmail()),
                firstNonBlank(invitation.getFirstName(), "Sin Nombre"),
                firstNonBlank(invitation.getLastName(), "Sin Apellido"));

        LocalDateTime createdAt = now();
        Usuario user = new Usuario();
        user.setIdCognito(cognitoUser.sub());
        user.setEmail(normalizeEmail(invitation.getEmail()));
        user.setNombre(firstNonBlank(invitation.getFirstName(), "Sin Nombre"));
        user.setApellido(firstNonBlank(invitation.getLastName(), "Sin Apellido"));
        user.setTelefono(normalizeOptional(invitation.getPhone()));
        user.setRol(RolUsuario.PADRE);
        user.setAutenticadoConCognito(true);
        user.setActivo(true);
        user.setCreadoEn(createdAt);
        user.setActualizadoEn(createdAt);
        return userRepository.save(user);
    }

    private void createTutorIfMissing(Usuario user) {
        Optional<Tutor> existingTutor = tutorRepository.findByUsuarioId(user.getId());
        if (existingTutor.isPresent()) {
            return;
        }

        Tutor tutor = new Tutor();
        tutor.setActiva(true);
        user.asignarTutor(tutor);
        userRepository.save(user);
    }

    private String normalizeUsername(String value) {
        if (isBlank(value)) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
