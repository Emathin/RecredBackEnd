package com.recred.backend.application;

import com.recred.backend.domain.exceptions.OperationNotAllowedException;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;
import com.recred.backend.domain.models.TutorSchool;
import com.recred.backend.domain.models.TutorSchoolStatus;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.TutorInvitationRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.TutorSchoolRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

public class AcceptTutorInvitation {

    private final UsuarioRepository userRepository;
    private final TutorRepository tutorRepository;
    private final TutorSchoolRepository tutorSchoolRepository;
    private final TutorInvitationRepository invitationRepository;
    private final TutorInvitationTokenService tokenService;
    private final Clock clock;

    public AcceptTutorInvitation(
            UsuarioRepository userRepository,
            TutorRepository tutorRepository,
            TutorSchoolRepository tutorSchoolRepository,
            TutorInvitationRepository invitationRepository,
            Clock clock) {
        this.userRepository = userRepository;
        this.tutorRepository = tutorRepository;
        this.tutorSchoolRepository = tutorSchoolRepository;
        this.invitationRepository = invitationRepository;
        this.tokenService = new TutorInvitationTokenService();
        this.clock = clock;
    }

    @Transactional
    public AcceptTutorInvitationResponse execute(AcceptTutorInvitationRequest acceptRequest) {
        TutorInvitation invitation = findInvitationByToken(acceptRequest.token());
        ensureInvitationCanBeAccepted(invitation);
        ensureAuthenticatedEmailMatchesInvitation(invitation, acceptRequest.authenticatedEmail());

        Usuario parentUser = findOrCreateParentUser(acceptRequest);
        Tutor tutor = findOrCreateTutor(parentUser);

        activateTutorSchoolAccess(tutor, invitation.getSchool());
        TutorInvitation acceptedInvitation = acceptInvitation(invitation);
        return AcceptTutorInvitationResponse.fromDomain(acceptedInvitation);
    }

    private TutorInvitation findInvitationByToken(String token) {
        return invitationRepository.findByTokenHash(tokenService.hash(token))
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la invitacion"));
    }

    private void ensureInvitationCanBeAccepted(TutorInvitation invitation) {
        if (invitation.getStatus() != TutorInvitationStatus.PENDING) {
            throw new IllegalArgumentException("La invitacion no esta pendiente");
        }

        if (invitationIsExpired(invitation)) {
            expireInvitation(invitation);
        }
    }

    private boolean invitationIsExpired(TutorInvitation invitation) {
        return invitation.getExpiresAt().isBefore(now());
    }

    private void expireInvitation(TutorInvitation invitation) {
        invitation.setStatus(TutorInvitationStatus.EXPIRED);
        invitationRepository.save(invitation);
        throw new IllegalArgumentException("La invitacion esta vencida");
    }

    private void ensureAuthenticatedEmailMatchesInvitation(
            TutorInvitation invitation,
            String authenticatedEmail) {
        String invitedEmail = normalizeEmail(invitation.getEmail());
        String actualEmail = normalizeEmail(authenticatedEmail);
        if (!invitedEmail.equals(actualEmail)) {
            throw new OperationNotAllowedException(
                    "El email autenticado no coincide con la invitacion");
        }
    }

    private Usuario findOrCreateParentUser(AcceptTutorInvitationRequest acceptRequest) {
        Optional<Usuario> existingUser = userRepository.findByIdCognito(
                acceptRequest.cognitoSubject());
        if (existingUser.isPresent()) {
            return ensureUserHasParentRole(existingUser.get());
        }

        return createParentUser(acceptRequest);
    }

    private Usuario ensureUserHasParentRole(Usuario user) {
        if (user.getRol() == RolUsuario.PADRE) {
            return user;
        }

        user.setRol(RolUsuario.PADRE);
        user.setActualizadoEn(now());
        return userRepository.save(user);
    }

    private Usuario createParentUser(AcceptTutorInvitationRequest acceptRequest) {
        LocalDateTime createdAt = now();
        Usuario user = new Usuario();
        user.setIdCognito(acceptRequest.cognitoSubject());
        user.setEmail(normalizeEmail(acceptRequest.authenticatedEmail()));
        user.setNombre(firstNonBlank(acceptRequest.firstName(), "Sin Nombre"));
        user.setApellido(firstNonBlank(acceptRequest.lastName(), "Sin Apellido"));
        user.setTelefono(normalizeOptional(acceptRequest.phone()));
        user.setDni(normalizeOptional(acceptRequest.documentNumber()));
        user.setRol(RolUsuario.PADRE);
        user.setAutenticadoConCognito(true);
        user.setActivo(true);
        user.setCreadoEn(createdAt);
        user.setActualizadoEn(createdAt);
        return userRepository.save(user);
    }

    private Tutor findOrCreateTutor(Usuario user) {
        return tutorRepository.findByUsuarioId(user.getId())
                .orElseGet(() -> createTutor(user));
    }

    private Tutor createTutor(Usuario user) {
        Tutor tutor = new Tutor();
        tutor.setActiva(true);
        user.asignarTutor(tutor);
        Usuario savedUser = userRepository.save(user);
        return tutorRepository.findByUsuarioId(savedUser.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "No se pudo crear el tutor para el usuario autenticado"));
    }

    private void activateTutorSchoolAccess(Tutor tutor, Colegio school) {
        TutorSchool tutorSchool = tutorSchoolRepository
                .findByTutorIdAndSchoolId(tutor.getId(), school.getId())
                .orElseGet(TutorSchool::new);
        tutorSchool.setTutorId(tutor.getId());
        tutorSchool.setSchool(school);
        tutorSchool.setStatus(TutorSchoolStatus.ACTIVE);
        tutorSchoolRepository.save(tutorSchool);
    }

    private TutorInvitation acceptInvitation(TutorInvitation invitation) {
        invitation.setStatus(TutorInvitationStatus.ACCEPTED);
        invitation.setUsedAt(now());
        return invitationRepository.save(invitation);
    }

    private String normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El email autenticado es obligatorio");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
