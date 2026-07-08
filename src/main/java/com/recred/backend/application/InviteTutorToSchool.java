package com.recred.backend.application;

import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.TutorInvitation;

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


public class InviteTutorToSchool {

    private static final Logger LOGGER = LoggerFactory.getLogger(InviteTutorToSchool.class);


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
    public TutorInvitationResult execute(InviteTutorToSchoolRequest request) {
        Colegio school = getSchoolForDirector(request.directorCognitoSubject());
        String email = formatEmail(request.email());

        if (isAlreadyAssociated(email, school.getId())) {
            return buildAlreadyAssociatedResult();
        }

        return createAndSendInvitation(school, email, request);
    }

    private Colegio getSchoolForDirector(String cognitoSubject) {
        Usuario director = findInvitingUser(cognitoSubject);
        return requireSchool(director);
    }

    private TutorInvitationResult buildAlreadyAssociatedResult() {
        return new TutorInvitationResult(null, null, "ALREADY_ASSOCIATED");
    }

    private TutorInvitationResult createAndSendInvitation(Colegio school, String email, 
                                                          InviteTutorToSchoolRequest request) {
        String token = tokenService.generateToken();
        TutorInvitation invitation = findOrCreateReusableInvitation(school.getId(), email);
        String resultStatus = determineStatus(invitation);

        setupInvitation(invitation, request, school, email, token);
        
        TutorInvitation saved = saveInvitation(invitation);
        String link = invitationLink(token);

        return sendEmailAndBuildResult(saved, link, resultStatus);
    }

    private String determineStatus(TutorInvitation invitation) {
        return invitation.getId() == null ? "CREATED" : "RESENT";
    }

    private void setupInvitation(TutorInvitation invitation, InviteTutorToSchoolRequest request, 
                                 Colegio school, String email, String token) {
        invitation.setupForInvitation(
                schoolRef(school.getId()),
                email,
                normalizeOptional(request.firstName()),
                normalizeOptional(request.lastName()),
                normalizeOptional(request.phone()),
                tokenService.hash(token),
                LocalDateTime.now(clock).plusDays(7)
        );
    }

    private TutorInvitationResult sendEmailAndBuildResult(TutorInvitation saved, String link, 
                                                          String baseStatus) {
        try {
            emailSender.sendInvitation(saved, link);
            return new TutorInvitationResult(saved, link, baseStatus);
        } catch (RuntimeException exception) {
            LOGGER.warn("No se pudo enviar por email la invitacion de tutor {}: {}",
                    saved.getId(), exception.getMessage());
            return new TutorInvitationResult(saved, link, baseStatus + "_EMAIL_ERROR");
        }
    }

    private TutorInvitation findOrCreateReusableInvitation(UUID schoolId, String email) {
        return invitationRepository.findReusableBySchoolIdAndEmail(schoolId, email)
                .orElseGet(TutorInvitation::new);
    }

    private TutorInvitation saveInvitation(TutorInvitation invitation) {
        return invitationRepository.save(invitation);
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

    private String formatEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
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

    public record TutorInvitationResult(
            TutorInvitation invitation,
            String invitationLink,
            String status
    ) {
    }
}
