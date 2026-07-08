package com.recred.backend.application;

import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;
import com.recred.backend.domain.models.TutorSchoolStatus;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.TutorInvitationRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.TutorSchoolRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.TutorInvitationEmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public class InviteTutorToSchool {

    private static final Logger LOGGER = LoggerFactory.getLogger(InviteTutorToSchool.class);
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
                    Pattern.CASE_INSENSITIVE);

    private final UsuarioRepository userRepository;
    private final TutorRepository tutorRepository;
    private final TutorSchoolRepository tutorSchoolRepository;
    private final TutorInvitationRepository invitationRepository;
    private final TutorInvitationEmailSender emailSender;
    private final TutorInvitationTokenService tokenService;
    private final Clock clock;
    private final String frontendBaseUrl;

    public InviteTutorToSchool(
            UsuarioRepository userRepository,
            TutorRepository tutorRepository,
            TutorSchoolRepository tutorSchoolRepository,
            TutorInvitationRepository invitationRepository,
            TutorInvitationEmailSender emailSender,
            Clock clock,
            String frontendBaseUrl) {
        this.userRepository = userRepository;
        this.tutorRepository = tutorRepository;
        this.tutorSchoolRepository = tutorSchoolRepository;
        this.invitationRepository = invitationRepository;
        this.emailSender = emailSender;
        this.tokenService = new TutorInvitationTokenService();
        this.clock = clock;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional
    public TutorInvitationResult execute(String directorCognitoSubject,
                                         TutorInvitationCommand command) {
        Usuario director = findInvitingUser(directorCognitoSubject);
        Colegio school = requireSchool(director);
        String email = normalizeEmail(command.email());

        if (isAlreadyAssociated(email, school.getId())) {
            return new TutorInvitationResult(null, null, "ALREADY_ASSOCIATED");
        }

        String token = tokenService.generateToken();
        TutorInvitation invitation = invitationRepository
                .findReusableBySchoolIdAndEmail(school.getId(), email)
                .orElseGet(TutorInvitation::new);
        String result = invitation.getId() == null ? "CREATED" : "RESENT";

        invitation.setSchool(schoolRef(school.getId()));
        invitation.setEmail(email);
        invitation.setFirstName(normalizeOptional(command.firstName()));
        invitation.setLastName(normalizeOptional(command.lastName()));
        invitation.setPhone(normalizeOptional(command.phone()));
        invitation.setTokenHash(tokenService.hash(token));
        invitation.setStatus(TutorInvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now(clock).plusDays(7));
        invitation.setUsedAt(null);

        TutorInvitation saved = invitationRepository.save(invitation);
        String link = invitationLink(token);

        try {
            emailSender.sendInvitation(saved, link);
        } catch (RuntimeException exception) {
            LOGGER.warn("No se pudo enviar por email la invitacion de tutor {}: {}",
                    saved.getId(), exception.getMessage());
            return new TutorInvitationResult(saved, link, result + "_EMAIL_ERROR");
        }

        return new TutorInvitationResult(saved, link, result);
    }

    private Usuario findInvitingUser(String cognitoSubject) {
        Usuario user = userRepository.findByIdCognito(cognitoSubject)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontro el usuario autenticado"));
        if (user.getRol() != RolUsuario.DIRECTIVO_COLEGIO
                && user.getRol() != RolUsuario.ADMIN) {
            throw new AccessDeniedException("El usuario autenticado no puede invitar tutores");
        }
        return user;
    }

    private Colegio requireSchool(Usuario user) {
        if (user.getColegio() == null || user.getColegio().getId() == null) {
            throw new IllegalArgumentException(
                    "El usuario autenticado no tiene un colegio asignado");
        }
        return user.getColegio();
    }

    private boolean isAlreadyAssociated(String email, UUID schoolId) {
        Optional<Usuario> existingUser = userRepository.findByEmail(email);
        if (existingUser.isEmpty()) {
            return false;
        }
        Optional<Tutor> existingTutor = tutorRepository.findByUsuarioId(existingUser.get().getId());
        return existingTutor.isPresent()
                && tutorSchoolRepository.existsByTutorIdAndSchoolIdAndStatus(
                        existingTutor.get().getId(),
                        schoolId,
                        TutorSchoolStatus.ACTIVE);
    }

    private Colegio schoolRef(UUID schoolId) {
        Colegio school = new Colegio();
        school.setId(schoolId);
        return school;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("El email no es valido");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String invitationLink(String token) {
        String base = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
        return base + "/invitaciones/tutor?token=" + token;
    }

    public record TutorInvitationCommand(
            String email,
            String firstName,
            String lastName,
            String phone
    ) {
    }

    public record TutorInvitationResult(
            TutorInvitation invitation,
            String invitationLink,
            String status
    ) {
    }
}
