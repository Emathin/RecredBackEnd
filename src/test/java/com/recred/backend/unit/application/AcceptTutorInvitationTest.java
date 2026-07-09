package com.recred.backend.unit.application;

import com.recred.backend.application.AcceptTutorInvitation;
import com.recred.backend.application.AcceptTutorInvitationRequest;
import com.recred.backend.application.AcceptTutorInvitationResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcceptTutorInvitationTest {

    private static final String TOKEN = "plain-token";
    private static final String COGNITO_SUBJECT = "parent-sub";
    private static final String EMAIL = "parent@test.com";
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-02T12:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private UsuarioRepository userRepository;
    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private TutorSchoolRepository tutorSchoolRepository;
    @Mock
    private TutorInvitationRepository invitationRepository;

    @Captor
    private ArgumentCaptor<Usuario> userCaptor;
    @Captor
    private ArgumentCaptor<TutorSchool> tutorSchoolCaptor;
    @Captor
    private ArgumentCaptor<TutorInvitation> invitationCaptor;

    private AcceptTutorInvitation useCase;
    private UUID schoolId;
    private UUID userId;
    private UUID tutorId;
    private TutorInvitation invitation;
    private AcceptTutorInvitationResponse response;
    private Throwable thrownException;

    @BeforeEach
    void setUp() {
        useCase = new AcceptTutorInvitation(
                userRepository,
                tutorRepository,
                tutorSchoolRepository,
                invitationRepository,
                CLOCK);
        schoolId = UUID.randomUUID();
        userId = UUID.randomUUID();
        tutorId = UUID.randomUUID();
    }

    @Test
    void newParentAcceptsPendingInvitationAndGetsSchoolAccess() {
        givenPendingInvitation();
        givenAuthenticatedUserDoesNotExist();
        givenParentUserIsSaved();
        givenTutorIsCreatedForSavedUser();
        givenTutorHasNoSchoolAccess();
        givenInvitationCanBeSaved();

        whenAcceptingInvitation();

        thenParentUserWasCreated();
        thenTutorSchoolAccessWasActivated();
        thenInvitationWasAccepted();
        thenResponseRepresentsAcceptedInvitation();
    }

    @Test
    void missingInvitationStopsAcceptance() {
        givenInvitationDoesNotExist();

        whenAcceptingInvitationFails(IllegalArgumentException.class);

        thenErrorMessageIs("No se encontro la invitacion");
        thenUserAndSchoolWereNotChanged();
    }

    @Test
    void alreadyAcceptedInvitationStopsAcceptance() {
        givenAlreadyAcceptedInvitation();

        whenAcceptingInvitationFails(IllegalArgumentException.class);

        thenErrorMessageIs("La invitacion no esta pendiente");
        thenUserAndSchoolWereNotChanged();
    }

    @Test
    void expiredInvitationIsMarkedAsExpiredBeforeFailing() {
        givenExpiredPendingInvitation();
        givenInvitationCanBeSaved();

        whenAcceptingInvitationFails(IllegalArgumentException.class);

        thenErrorMessageIs("La invitacion esta vencida");
        thenInvitationWasMarkedExpired();
        thenUserAndSchoolWereNotChanged();
    }

    @Test
    void authenticatedEmailMustMatchInvitationEmail() {
        givenPendingInvitation();

        whenAcceptingInvitationFails(
                requestWithAuthenticatedEmail("other@test.com"),
                OperationNotAllowedException.class);

        thenErrorMessageIs("El email autenticado no coincide con la invitacion");
        thenUserAndSchoolWereNotChanged();
    }

    @Test
    void existingNonParentUserIsPromotedAndExistingSchoolAccessIsActivated() {
        givenPendingInvitation();
        givenExistingUserWithRole(RolUsuario.ADMIN);
        givenTutorExistsForUser();
        givenRevokedTutorSchoolAccess();
        givenInvitationCanBeSaved();

        whenAcceptingInvitation();

        thenExistingUserWasPromotedToParent();
        thenTutorSchoolAccessWasActivated();
        thenInvitationWasAccepted();
    }

    @Test
    void givenAccountCreatedInvitation_whenAcceptingAfterLogin_thenActivatesSchoolAccess() {
        givenPendingInvitation();
        givenExistingParentUser();
        givenTutorExistsForUser();
        givenTutorHasNoSchoolAccess();
        givenInvitationCanBeSaved();

        whenAcceptingInvitation();

        thenTutorSchoolAccessWasActivated();
        thenInvitationWasAccepted();
        thenResponseRepresentsAcceptedInvitation();
    }

    @Test
    void givenAccountCreatedInvitationAndJwtWithoutEmail_whenAcceptingAfterLogin_thenUsesLocalUserEmail() {
        givenPendingInvitation();
        givenExistingParentUserWithEmail();
        givenTutorExistsForUser();
        givenTutorHasNoSchoolAccess();
        givenInvitationCanBeSaved();

        whenAcceptingInvitation(requestWithoutAuthenticatedEmail());

        thenTutorSchoolAccessWasActivated();
        thenInvitationWasAccepted();
    }

    @Test
    void givenPendingInvitationAndJwtWithoutEmail_whenAcceptingAfterTemporaryLogin_thenUsesInvitationEmail() {
        givenPendingInvitation();
        givenAuthenticatedUserDoesNotExist();
        givenParentUserIsSaved();
        givenTutorIsCreatedForSavedUser();
        givenTutorHasNoSchoolAccess();
        givenInvitationCanBeSaved();

        whenAcceptingInvitation(requestWithoutAuthenticatedEmail());

        thenParentUserWasCreated();
        thenTutorSchoolAccessWasActivated();
        thenInvitationWasAccepted();
    }

    @Test
    void newParentWithoutProfileDataUsesFallbackValues() {
        givenPendingInvitation();
        givenAuthenticatedUserDoesNotExist();
        givenParentUserIsSaved();
        givenTutorIsCreatedForSavedUser();
        givenTutorHasNoSchoolAccess();
        givenInvitationCanBeSaved();

        whenAcceptingInvitation(requestWithoutProfileData());

        thenParentUserWasCreatedWithFallbackProfile();
    }

    private void givenPendingInvitation() {
        invitation = invitation(TutorInvitationStatus.PENDING);
        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation));
    }


    private void givenInvitationDoesNotExist() {
        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.empty());
    }

    private void givenAlreadyAcceptedInvitation() {
        invitation = invitation(TutorInvitationStatus.ACCEPTED);
        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation));
    }

    private void givenExpiredPendingInvitation() {
        invitation = invitation(TutorInvitationStatus.PENDING);
        invitation.setExpiresAt(now().minusMinutes(1));
        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation));
    }

    private void givenAuthenticatedUserDoesNotExist() {
        when(userRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());
    }

    private void givenExistingUserWithRole(RolUsuario role) {
        Usuario user = user(role);
        when(userRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
    }

    private void givenExistingParentUser() {
        when(userRepository.findByIdCognito(COGNITO_SUBJECT))
                .thenReturn(Optional.of(user(RolUsuario.PADRE)));
    }

    private void givenExistingParentUserWithEmail() {
        Usuario user = user(RolUsuario.PADRE);
        user.setEmail(EMAIL);
        when(userRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(user));
    }

    private void givenParentUserIsSaved() {
        when(userRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });
    }

    private void givenTutorIsCreatedForSavedUser() {
        Tutor tutor = tutor();
        when(tutorRepository.findByUsuarioId(userId))
                .thenReturn(Optional.empty(), Optional.of(tutor));
    }

    private void givenTutorExistsForUser() {
        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.of(tutor()));
    }

    private void givenTutorHasNoSchoolAccess() {
        when(tutorSchoolRepository.findByTutorIdAndSchoolId(tutorId, schoolId))
                .thenReturn(Optional.empty());
    }

    private void givenRevokedTutorSchoolAccess() {
        TutorSchool tutorSchool = new TutorSchool();
        tutorSchool.setStatus(TutorSchoolStatus.REVOKED);
        when(tutorSchoolRepository.findByTutorIdAndSchoolId(tutorId, schoolId))
                .thenReturn(Optional.of(tutorSchool));
    }

    private void givenInvitationCanBeSaved() {
        when(invitationRepository.save(any(TutorInvitation.class))).thenAnswer(invocation ->
                invocation.getArgument(0));
    }

    private void whenAcceptingInvitation() {
        whenAcceptingInvitation(defaultRequest());
    }

    private void whenAcceptingInvitation(AcceptTutorInvitationRequest acceptRequest) {
        response = useCase.execute(acceptRequest);
    }

    private <T extends Throwable> void whenAcceptingInvitationFails(Class<T> exceptionType) {
        whenAcceptingInvitationFails(defaultRequest(), exceptionType);
    }

    private <T extends Throwable> void whenAcceptingInvitationFails(
            AcceptTutorInvitationRequest acceptRequest,
            Class<T> exceptionType) {
        Executable execution = () -> useCase.execute(acceptRequest);
        thrownException = assertThrows(exceptionType, execution);
    }

    private void thenParentUserWasCreated() {
        verify(userRepository, times(2)).save(userCaptor.capture());
        Usuario savedUser = userCaptor.getAllValues().get(0);
        assertEquals(RolUsuario.PADRE, savedUser.getRol());
        assertEquals(EMAIL, savedUser.getEmail());
    }

    private void thenExistingUserWasPromotedToParent() {
        verify(userRepository).save(userCaptor.capture());
        Usuario savedUser = userCaptor.getValue();
        assertEquals(RolUsuario.PADRE, savedUser.getRol());
        assertEquals(now(), savedUser.getActualizadoEn());
    }

    private void thenParentUserWasCreatedWithFallbackProfile() {
        verify(userRepository, times(2)).save(userCaptor.capture());
        Usuario savedUser = userCaptor.getAllValues().get(0);
        assertEquals("Sin Nombre", savedUser.getNombre());
        assertEquals("Sin Apellido", savedUser.getApellido());
        assertNull(savedUser.getTelefono());
        assertNull(savedUser.getDni());
    }

    private void thenTutorSchoolAccessWasActivated() {
        verify(tutorSchoolRepository).save(tutorSchoolCaptor.capture());
        TutorSchool tutorSchool = tutorSchoolCaptor.getValue();
        assertEquals(tutorId, tutorSchool.getTutorId());
        assertEquals(schoolId, tutorSchool.getSchool().getId());
        assertEquals(TutorSchoolStatus.ACTIVE, tutorSchool.getStatus());
    }

    private void thenInvitationWasAccepted() {
        verify(invitationRepository).save(invitationCaptor.capture());
        TutorInvitation savedInvitation = invitationCaptor.getValue();
        assertEquals(TutorInvitationStatus.ACCEPTED, savedInvitation.getStatus());
        assertEquals(now(), savedInvitation.getUsedAt());
    }

    private void thenInvitationWasMarkedExpired() {
        verify(invitationRepository).save(invitationCaptor.capture());
        assertEquals(TutorInvitationStatus.EXPIRED, invitationCaptor.getValue().getStatus());
    }

    private void thenResponseRepresentsAcceptedInvitation() {
        assertEquals(invitation.getId(), response.id());
        assertEquals(schoolId, response.schoolId());
        assertEquals(TutorInvitationStatus.ACCEPTED, response.status());
    }

    private void thenErrorMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private void thenUserAndSchoolWereNotChanged() {
        verifyNoInteractions(userRepository, tutorRepository, tutorSchoolRepository);
    }

    private AcceptTutorInvitationRequest defaultRequest() {
        return new AcceptTutorInvitationRequest(
                TOKEN,
                COGNITO_SUBJECT,
                " Parent@Test.com ",
                "Ana",
                "Gomez",
                "1111",
                "123");
    }

    private AcceptTutorInvitationRequest requestWithAuthenticatedEmail(String email) {
        return new AcceptTutorInvitationRequest(
                TOKEN,
                COGNITO_SUBJECT,
                email,
                "Ana",
                "Gomez",
                null,
                null
        );
    }

    private AcceptTutorInvitationRequest requestWithoutProfileData() {
        return new AcceptTutorInvitationRequest(
                TOKEN,
                COGNITO_SUBJECT,
                EMAIL,
                " ",
                null,
                " ",
                ""
        );
    }

    private AcceptTutorInvitationRequest requestWithoutAuthenticatedEmail() {
        return new AcceptTutorInvitationRequest(
                TOKEN,
                COGNITO_SUBJECT,
                null,
                "Ana",
                "Gomez",
                "1111",
                "123");
    }

    private TutorInvitation invitation(TutorInvitationStatus status) {
        TutorInvitation tutorInvitation = new TutorInvitation();
        tutorInvitation.setId(UUID.randomUUID());
        tutorInvitation.setEmail(EMAIL);
        tutorInvitation.setStatus(status);
        tutorInvitation.setExpiresAt(now().plusDays(1));
        tutorInvitation.setSchool(school());
        return tutorInvitation;
    }

    private Usuario user(RolUsuario role) {
        Usuario user = new Usuario();
        user.setId(userId);
        user.setRol(role);
        return user;
    }

    private Tutor tutor() {
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        return tutor;
    }

    private Colegio school() {
        Colegio school = new Colegio();
        school.setId(schoolId);
        return school;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(CLOCK);
    }
}
