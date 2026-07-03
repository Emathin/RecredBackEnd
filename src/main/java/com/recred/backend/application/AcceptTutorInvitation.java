package com.recred.backend.application;

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
import org.springframework.security.access.AccessDeniedException;
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
    public TutorInvitation execute(AcceptTutorInvitationRequest request) {
        TutorInvitation invitation = invitationRepository
                .findByTokenHash(tokenService.hash(request.token()))
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la invitacion"));
        validateInvitation(invitation);
        validateEmail(invitation, request.authenticatedEmail());

        Usuario user = findOrCreateUser(request);
        Tutor tutor = tutorRepository.findByUsuarioId(user.getId())
                .orElseGet(() -> createTutor(user));

        activateTutorSchool(tutor, invitation.getSchool());
        invitation.setStatus(TutorInvitationStatus.ACCEPTED);
        invitation.setUsedAt(LocalDateTime.now(clock));
        return invitationRepository.save(invitation);
    }

    private void validateInvitation(TutorInvitation invitation) {
        if (invitation.getStatus() != TutorInvitationStatus.PENDING) {
            throw new IllegalArgumentException("La invitacion no esta pendiente");
        }
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now(clock))) {
            invitation.setStatus(TutorInvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new IllegalArgumentException("La invitacion esta vencida");
        }
    }

    private void validateEmail(TutorInvitation invitation, String authenticatedEmail) {
        String invitedEmail = normalizeEmail(invitation.getEmail());
        String actualEmail = normalizeEmail(authenticatedEmail);
        if (!invitedEmail.equals(actualEmail)) {
            throw new AccessDeniedException(
                    "El email autenticado no coincide con la invitacion");
        }
    }

    private Usuario findOrCreateUser(AcceptTutorInvitationRequest request) {
        Optional<Usuario> existing = userRepository.findByIdCognito(request.cognitoSubject());
        if (existing.isPresent()) {
            Usuario user = existing.get();
            if (user.getRol() != RolUsuario.PADRE) {
                user.setRol(RolUsuario.PADRE);
                user.setActualizadoEn(LocalDateTime.now(clock));
                return userRepository.save(user);
            }
            return user;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Usuario user = new Usuario();
        user.setIdCognito(request.cognitoSubject());
        user.setEmail(normalizeEmail(request.authenticatedEmail()));
        user.setNombre(firstNonBlank(request.firstName(), "Sin Nombre"));
        user.setApellido(firstNonBlank(request.lastName(), "Sin Apellido"));
        user.setTelefono(normalizeOptional(request.phone()));
        user.setDni(normalizeOptional(request.documentNumber()));
        user.setRol(RolUsuario.PADRE);
        user.setAutenticadoConCognito(true);
        user.setActivo(true);
        user.setCreadoEn(now);
        user.setActualizadoEn(now);
        return userRepository.save(user);
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

    private void activateTutorSchool(Tutor tutor, Colegio school) {
        TutorSchool tutorSchool = tutorSchoolRepository
                .findByTutorIdAndSchoolId(tutor.getId(), school.getId())
                .orElseGet(TutorSchool::new);
        tutorSchool.setTutorId(tutor.getId());
        tutorSchool.setSchool(school);
        tutorSchool.setStatus(TutorSchoolStatus.ACTIVE);
        tutorSchoolRepository.save(tutorSchool);
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
}
