package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.AcceptTutorInvitation;
import com.recred.backend.application.AcceptTutorInvitationRequest;
import com.recred.backend.application.GetTutorInvitation;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;
import com.recred.backend.infrastructure.web.TutorInvitationController;
import com.recred.backend.infrastructure.web.TutorInvitationResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TutorInvitationControllerTest {

    @Mock
    private GetTutorInvitation getTutorInvitation;

    @Mock
    private AcceptTutorInvitation acceptTutorInvitation;

    private TutorInvitationController controller;

    @BeforeEach
    void setUp() {
        controller = new TutorInvitationController(getTutorInvitation, acceptTutorInvitation);
    }

    @Test
    void getInvitationShouldReturnValidatedInvitation() {
        TutorInvitation invitation = invitation();
        when(getTutorInvitation.execute("token-123")).thenReturn(invitation);

        ResponseEntity<TutorInvitationResponse> response =
                controller.getInvitation("token-123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(invitation.getId(), response.getBody().id());
        assertEquals("VALIDATED", response.getBody().result());
        verify(getTutorInvitation).execute("token-123");
    }

    @Test
    void acceptInvitationShouldMapJwtClaimsAndReturnAcceptedInvitation() {
        TutorInvitation invitation = invitation();
        when(acceptTutorInvitation.execute(
                org.mockito.ArgumentMatchers.any(AcceptTutorInvitationRequest.class)))
                .thenReturn(invitation);

        ResponseEntity<TutorInvitationResponse> response = controller.acceptInvitation(
                "token-123",
                jwt(Map.of(
                        "sub", "tutor-sub",
                        "email", "tutor@example.com",
                        "given_name", "  ",
                        "name", " Ana Gomez ",
                        "family_name", "Gomez",
                        "phone_number", "1122334455",
                        "custom:document_number", "12345678"
                ))
        );

        ArgumentCaptor<AcceptTutorInvitationRequest> captor =
                ArgumentCaptor.forClass(AcceptTutorInvitationRequest.class);
        verify(acceptTutorInvitation).execute(captor.capture());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ACCEPTED", response.getBody().result());
        assertEquals("token-123", captor.getValue().token());
        assertEquals("tutor-sub", captor.getValue().cognitoSubject());
        assertEquals("Ana Gomez", captor.getValue().firstName());
        assertEquals("Gomez", captor.getValue().lastName());
        assertEquals("12345678", captor.getValue().documentNumber());
    }

    @Test
    void acceptInvitationShouldAllowMissingNameClaims() {
        TutorInvitation invitation = invitation();
        when(acceptTutorInvitation.execute(
                org.mockito.ArgumentMatchers.any(AcceptTutorInvitationRequest.class)))
                .thenReturn(invitation);

        controller.acceptInvitation(
                "token-123",
                jwt(Map.of(
                        "sub", "tutor-sub",
                        "email", "tutor@example.com"
                ))
        );

        ArgumentCaptor<AcceptTutorInvitationRequest> captor =
                ArgumentCaptor.forClass(AcceptTutorInvitationRequest.class);
        verify(acceptTutorInvitation).execute(captor.capture());
        assertNull(captor.getValue().firstName());
    }

    @Test
    void acceptInvitationShouldPreferGivenNameClaim() {
        TutorInvitation invitation = invitation();
        when(acceptTutorInvitation.execute(
                org.mockito.ArgumentMatchers.any(AcceptTutorInvitationRequest.class)))
                .thenReturn(invitation);

        controller.acceptInvitation(
                "token-123",
                jwt(Map.of(
                        "sub", "tutor-sub",
                        "email", "tutor@example.com",
                        "given_name", " Ana ",
                        "name", "Nombre completo"
                ))
        );

        ArgumentCaptor<AcceptTutorInvitationRequest> captor =
                ArgumentCaptor.forClass(AcceptTutorInvitationRequest.class);
        verify(acceptTutorInvitation).execute(captor.capture());
        assertEquals("Ana", captor.getValue().firstName());
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

    private TutorInvitation invitation() {
        TutorInvitation invitation = new TutorInvitation();
        invitation.setId(UUID.randomUUID());
        invitation.setSchool(school());
        invitation.setEmail("tutor@example.com");
        invitation.setFirstName("Ana");
        invitation.setLastName("Gomez");
        invitation.setPhone("1122334455");
        invitation.setStatus(TutorInvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.of(2026, 7, 8, 10, 0));
        return invitation;
    }

    private Colegio school() {
        Colegio school = new Colegio();
        school.setId(UUID.randomUUID());
        school.setNombre("Colegio");
        return school;
    }
}
