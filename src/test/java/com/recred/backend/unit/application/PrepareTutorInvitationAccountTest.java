package com.recred.backend.unit.application;

import com.recred.backend.application.PrepareTutorInvitationAccount;
import com.recred.backend.application.PrepareTutorInvitationAccountRequest;
import com.recred.backend.application.PrepareTutorInvitationAccountResponse;
import com.recred.backend.domain.exceptions.UsernameExistsException;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.TutorInvitationRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.CognitoTutorIdentityService;
import com.recred.backend.domain.services.CreatedCognitoUser;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrepareTutorInvitationAccountTest {

    private static final String TOKEN = "plain-token";
    private static final String EMAIL = "parent@test.com";
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-02T12:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private UsuarioRepository userRepository;
    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private TutorInvitationRepository invitationRepository;
    @Mock
    private CognitoTutorIdentityService cognitoService;

    @Captor
    private ArgumentCaptor<Usuario> userCaptor;
    @Captor
    private ArgumentCaptor<TutorInvitation> invitationCaptor;
    @Captor
    private ArgumentCaptor<String> usernameCaptor;

    private PrepareTutorInvitationAccount useCase;
    private TutorInvitation invitation;
    private PrepareTutorInvitationAccountResponse response;
    private Throwable thrownException;
    private UUID userId;

    @BeforeEach
    void setUp() {
        useCase = new PrepareTutorInvitationAccount(
                userRepository,
                tutorRepository,
                invitationRepository,
                cognitoService,
                CLOCK);
        userId = UUID.randomUUID();
    }

    @Test
    void givenMissingUserAndNoUsername_whenPreparingAccount_thenRequiresUsername() {
        givenPendingInvitation();
        givenUserEmailDoesNotExist();

        whenPreparingTutorInvitationAccount();

        thenResultRequiresUsername();
        thenCognitoAndTutorWereNotChanged();
    }

    @Test
    void givenMissingUserAndUsername_whenPreparingAccount_thenCreatesParentAccountWithoutSchoolAccess() {
        givenPendingInvitation();
        givenUserEmailDoesNotExist();
        givenCognitoCreatesUser();
        givenUserIsSaved();
        givenTutorDoesNotExist();

        whenPreparingTutorInvitationAccount("parent");

        thenCognitoTutorUserWasCreated();
        thenParentUserWasCreated();
        thenTutorWasCreatedThroughUser();
        thenInvitationWasNotMarkedAsAccountCreated();
        thenResultIsAccountCreated();
    }

    @Test
    void givenExistingUserByEmail_whenPreparingAccount_thenRequiresLogin() {
        givenPendingInvitation();
        givenUserEmailAlreadyExists();

        whenPreparingTutorInvitationAccount();

        thenResultRequiresLogin();
        thenCognitoAndTutorWereNotChanged();
    }

    @Test
    void givenCognitoUserAlreadyExists_whenPreparingAccount_thenRequiresLogin() {
        givenPendingInvitation();
        givenUserEmailDoesNotExist();
        givenCognitoUserAlwaysExists();

        whenPreparingTutorInvitationAccountFails(IllegalArgumentException.class, "parent");

        thenErrorMessageIs("El nombre de usuario ya esta en uso");
    }


    @Test
    void givenExpiredInvitation_whenPreparingAccount_thenMarksInvitationExpired() {
        givenExpiredPendingInvitation();
        givenInvitationCanBeSaved();

        whenPreparingTutorInvitationAccountFails(IllegalArgumentException.class);

        thenErrorMessageIs("La invitacion esta vencida");
        thenInvitationWasMarkedExpired();
    }

    private void givenPendingInvitation() {
        invitation = invitation(TutorInvitationStatus.PENDING);
        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation));
    }

    private void givenExpiredPendingInvitation() {
        invitation = invitation(TutorInvitationStatus.PENDING);
        invitation.setExpiresAt(now().minusMinutes(1));
        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation));
    }

    private void givenUserEmailDoesNotExist() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
    }

    private void givenUserEmailAlreadyExists() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(new Usuario()));
    }

    private void givenCognitoCreatesUser() {
        when(cognitoService.createTutorUser(any(), eq(EMAIL), eq("Ana"), eq("Gomez")))
                .thenReturn(new CreatedCognitoUser("parent-sub", EMAIL));
    }

    private void givenCognitoUserAlwaysExists() {
        when(cognitoService.createTutorUser(any(), any(), any(), any()))
                .thenThrow(new UsernameExistsException("exists"));
    }

    private void givenUserIsSaved() {
        when(userRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });
    }

    private void givenTutorDoesNotExist() {
        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.empty());
    }

    private void givenInvitationCanBeSaved() {
        when(invitationRepository.save(any(TutorInvitation.class))).thenAnswer(invocation ->
                invocation.getArgument(0));
    }

    private void whenPreparingTutorInvitationAccount() {
        response = useCase.execute(requestWithoutUsername());
    }

    private void whenPreparingTutorInvitationAccount(String username) {
        response = useCase.execute(requestWithUsername(username));
    }

    private <T extends Throwable> void whenPreparingTutorInvitationAccountFails(
            Class<T> expectedException) {
        Executable execution = () -> useCase.execute(requestWithoutUsername());
        thrownException = assertThrows(expectedException, execution);
    }

    private <T extends Throwable> void whenPreparingTutorInvitationAccountFails(
            Class<T> expectedException, String username) {
        Executable execution = () -> useCase.execute(requestWithUsername(username));
        thrownException = assertThrows(expectedException, execution);
    }

    private PrepareTutorInvitationAccountRequest requestWithoutUsername() {
        return new PrepareTutorInvitationAccountRequest(TOKEN, null);
    }

    private PrepareTutorInvitationAccountRequest requestWithUsername(String username) {
        return new PrepareTutorInvitationAccountRequest(TOKEN, username);
    }

    private void thenCognitoTutorUserWasCreated() {
        verify(cognitoService).createTutorUser(
                usernameCaptor.capture(), eq(EMAIL), eq("Ana"), eq("Gomez"));
        assertEquals("parent", usernameCaptor.getValue());
        assertFalse(usernameCaptor.getValue().contains("@"));
    }

    private void thenParentUserWasCreated() {
        verify(userRepository, times(2)).save(userCaptor.capture());
        Usuario savedUser = userCaptor.getAllValues().get(0);
        assertEquals(RolUsuario.PADRE, savedUser.getRol());
        assertEquals("parent-sub", savedUser.getIdCognito());
        assertEquals(EMAIL, savedUser.getEmail());
    }

    private void thenTutorWasCreatedThroughUser() {
        Usuario savedUserWithTutor = userCaptor.getAllValues().get(1);
        assertEquals(Boolean.TRUE, savedUserWithTutor.getTutor().getActiva());
    }

    private void thenInvitationWasNotMarkedAsAccountCreated() {
        assertEquals(TutorInvitationStatus.PENDING, invitation.getStatus());
    }

    private void thenInvitationWasMarkedExpired() {
        verify(invitationRepository).save(invitationCaptor.capture());
        assertEquals(TutorInvitationStatus.EXPIRED, invitationCaptor.getValue().getStatus());
    }

    private void thenResultIsAccountCreated() {
        assertEquals(PrepareTutorInvitationAccount.ACCOUNT_CREATED_TEMPORARY_PASSWORD_SENT,
                response.result());
    }

    private void thenResultRequiresLogin() {
        assertEquals(PrepareTutorInvitationAccount.LOGIN_REQUIRED, response.result());
    }

    private void thenResultRequiresUsername() {
        assertEquals(PrepareTutorInvitationAccount.USERNAME_REQUIRED, response.result());
    }

    private void thenCognitoAndTutorWereNotChanged() {
        verifyNoInteractions(cognitoService, tutorRepository);
    }

    private void thenErrorMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private TutorInvitation invitation(TutorInvitationStatus status) {
        TutorInvitation tutorInvitation = new TutorInvitation();
        tutorInvitation.setId(UUID.randomUUID());
        tutorInvitation.setEmail(EMAIL);
        tutorInvitation.setFirstName("Ana");
        tutorInvitation.setLastName("Gomez");
        tutorInvitation.setPhone("1111");
        tutorInvitation.setStatus(status);
        tutorInvitation.setExpiresAt(now().plusDays(1));
        tutorInvitation.setSchool(school());
        return tutorInvitation;
    }

    private Colegio school() {
        Colegio school = new Colegio();
        school.setId(UUID.randomUUID());
        school.setNombre("Colegio");
        return school;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(CLOCK);
    }
}
