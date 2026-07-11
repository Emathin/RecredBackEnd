package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.AcceptTutorInvitation;
import com.recred.backend.application.AcceptTutorInvitationRequest;
import com.recred.backend.application.AcceptTutorInvitationResponse;
import com.recred.backend.application.GetTutorInvitation;
import com.recred.backend.application.PrepareTutorInvitationAccount;
import com.recred.backend.application.PrepareTutorInvitationAccountRequest;
import com.recred.backend.application.PrepareTutorInvitationAccountResponse;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;
import com.recred.backend.infrastructure.web.PrepareTutorInvitationAccountRequestDTO;
import com.recred.backend.infrastructure.web.TutorInvitationController;
import com.recred.backend.infrastructure.web.TutorInvitationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TutorInvitationControllerTest {

    @Mock
    private GetTutorInvitation getTutorInvitation;
    @Mock
    private AcceptTutorInvitation acceptTutorInvitation;
    @Mock
    private PrepareTutorInvitationAccount prepareTutorInvitationAccount;

    private TutorInvitationController controller;

    private TutorInvitation invitation;
    private UUID invitationId;
    private UUID schoolId;

    private ResponseEntity<PrepareTutorInvitationAccountResponse> prepareResponse;
    private ResponseEntity<TutorInvitationResponse> invitationResponse;

    @BeforeEach
    void setUp() {
        controller = new TutorInvitationController(
                getTutorInvitation,
                acceptTutorInvitation,
                prepareTutorInvitationAccount);
        invitationId = UUID.randomUUID();
        schoolId = UUID.randomUUID();
        invitation = createDefaultInvitation();
    }

    @Test
    void givenValidRequestWhenPreparingAccountThenDelegatesToUseCase() {
        givenPrepareUseCaseReturnsLoginRequired();

        whenPreparingAccount("token-123", "tutoruser");

        thenPrepareUseCaseWasExecutedWith("token-123", "tutoruser");
        thenPrepareResponseIsLoginRequired();
    }

    @Test
    void givenValidTokenWhenGettingInvitationThenReturnsValidatedInvitation() {
        givenGetUseCaseReturnsInvitation("token-123");

        whenGettingInvitation("token-123");

        thenGetInvitationResponseIsValidated();
        thenGetUseCaseWasExecutedWith("token-123");
    }

    @Test
    void givenFullClaimsWhenAcceptingInvitationThenMapsClaimsAndReturnsAcceptedInvitation() {
        givenAcceptUseCaseSucceeds();

        whenAcceptingInvitation("token-123", fullClaimsJwt());

        thenAcceptInvitationResponseIsAccepted();
        thenAcceptUseCaseWasExecutedWithExpectedRequest();
    }

    @Test
    void givenMinimalClaimsWhenAcceptingInvitationThenAllowsMissingNameClaims() {
        givenAcceptUseCaseSucceeds();

        whenAcceptingInvitation("token-123", minimalClaimsJwt());

        thenAcceptUseCaseWasExecutedWithMissingFirstName();
    }

    @Test
    void givenBothNameClaimsWhenAcceptingInvitationThenPrefersGivenNameClaim() {
        givenAcceptUseCaseSucceeds();

        whenAcceptingInvitation("token-123", jwtWithGivenNameAndFullName());

        thenAcceptUseCaseWasExecutedWithPreferredFirstName("Ana");
    }

    private void givenPrepareUseCaseReturnsLoginRequired() {
        PrepareTutorInvitationAccountResponse useCaseResponse =
                new PrepareTutorInvitationAccountResponse(
                        invitationId,
                        schoolId,
                        "Colegio",
                        "tutor@example.com",
                        PrepareTutorInvitationAccount.LOGIN_REQUIRED);
        when(prepareTutorInvitationAccount.execute(any(PrepareTutorInvitationAccountRequest.class)))
                .thenReturn(useCaseResponse);
    }

    private void givenGetUseCaseReturnsInvitation(String token) {
        when(getTutorInvitation.execute(token)).thenReturn(
                com.recred.backend.application.GetTutorInvitationResponse.fromDomain(invitation)
        );
    }

    private void givenAcceptUseCaseSucceeds() {
        when(acceptTutorInvitation.execute(any(AcceptTutorInvitationRequest.class)))
                .thenReturn(AcceptTutorInvitationResponse.fromDomain(invitation));
    }

    private void whenPreparingAccount(String token, String username) {
        prepareResponse = controller.prepareAccount(
                token,
                new PrepareTutorInvitationAccountRequestDTO(username));
    }

    private void whenGettingInvitation(String token) {
        invitationResponse = controller.getInvitation(token);
    }

    private void whenAcceptingInvitation(String token, Jwt jwt) {
        invitationResponse = controller.acceptInvitation(token, jwt);
    }

    private void thenPrepareUseCaseWasExecutedWith(String expectedToken, String expectedUsername) {
        ArgumentCaptor<PrepareTutorInvitationAccountRequest> captor =
                ArgumentCaptor.forClass(PrepareTutorInvitationAccountRequest.class);
        verify(prepareTutorInvitationAccount).execute(captor.capture());
        assertEquals(expectedToken, captor.getValue().token());
        assertEquals(expectedUsername, captor.getValue().username());
    }

    private void thenPrepareResponseIsLoginRequired() {
        assertEquals(HttpStatus.OK, prepareResponse.getStatusCode());
        assertEquals(PrepareTutorInvitationAccount.LOGIN_REQUIRED, prepareResponse.getBody().result());
    }

    private void thenGetUseCaseWasExecutedWith(String expectedToken) {
        verify(getTutorInvitation).execute(expectedToken);
    }

    private void thenGetInvitationResponseIsValidated() {
        assertEquals(HttpStatus.OK, invitationResponse.getStatusCode());
        assertEquals(invitation.getId(), invitationResponse.getBody().id());
        assertEquals("VALIDATED", invitationResponse.getBody().result());
    }

    private void thenAcceptInvitationResponseIsAccepted() {
        assertEquals(HttpStatus.OK, invitationResponse.getStatusCode());
        assertEquals("ACCEPTED", invitationResponse.getBody().result());
    }

    private void thenAcceptUseCaseWasExecutedWithExpectedRequest() {
        AcceptTutorInvitationRequest request = captureAcceptRequest();
        assertEquals("token-123", request.token());
        assertEquals("tutor-sub", request.cognitoSubject());
        assertEquals("Ana Gomez", request.firstName());
        assertEquals("Gomez", request.lastName());
        assertEquals("12345678", request.documentNumber());
    }

    private void thenAcceptUseCaseWasExecutedWithMissingFirstName() {
        AcceptTutorInvitationRequest request = captureAcceptRequest();
        assertNull(request.firstName());
    }

    private void thenAcceptUseCaseWasExecutedWithPreferredFirstName(String expectedFirstName) {
        AcceptTutorInvitationRequest request = captureAcceptRequest();
        assertEquals(expectedFirstName, request.firstName());
    }

    private AcceptTutorInvitationRequest captureAcceptRequest() {
        ArgumentCaptor<AcceptTutorInvitationRequest> captor =
                ArgumentCaptor.forClass(AcceptTutorInvitationRequest.class);
        verify(acceptTutorInvitation).execute(captor.capture());
        return captor.getValue();
    }

    private Jwt fullClaimsJwt() {
        return jwt(Map.of(
                "sub", "tutor-sub",
                "email", "tutor@example.com",
                "given_name", "  ",
                "name", " Ana Gomez ",
                "family_name", "Gomez",
                "phone_number", "1122334455",
                "custom:document_number", "12345678"
        ));
    }

    private Jwt minimalClaimsJwt() {
        return jwt(Map.of(
                "sub", "tutor-sub",
                "email", "tutor@example.com"
        ));
    }

    private Jwt jwtWithGivenNameAndFullName() {
        return jwt(Map.of(
                "sub", "tutor-sub",
                "email", "tutor@example.com",
                "given_name", " Ana ",
                "name", "Nombre completo"
        ));
    }

    private Jwt jwt(Map<String, Object> claims) {
        return new Jwt(
                "token-value",
                Instant.parse("2026-07-01T10:00:00Z"),
                Instant.parse("2026-07-01T11:00:00Z"),
                Map.of("alg", "none"),
                claims
        );
    }

    private TutorInvitation createDefaultInvitation() {
        TutorInvitation tutorInvitation = new TutorInvitation();
        tutorInvitation.setId(invitationId);
        tutorInvitation.setSchool(createDefaultSchool());
        tutorInvitation.setEmail("tutor@example.com");
        tutorInvitation.setFirstName("Ana");
        tutorInvitation.setLastName("Gomez");
        tutorInvitation.setPhone("1122334455");
        tutorInvitation.setStatus(TutorInvitationStatus.PENDING);
        tutorInvitation.setExpiresAt(LocalDateTime.of(2026, 7, 8, 10, 0));
        return tutorInvitation;
    }

    private Colegio createDefaultSchool() {
        Colegio school = new Colegio();
        school.setId(schoolId);
        school.setNombre("Colegio");
        return school;
    }
}
