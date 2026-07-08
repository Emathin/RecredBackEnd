package com.recred.backend.unit.application;

import com.recred.backend.application.GetTutorInvitation;
import com.recred.backend.application.GetTutorInvitationResponse;
import com.recred.backend.application.TutorInvitationTokenService;
import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;
import com.recred.backend.domain.repositories.TutorInvitationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTutorInvitationTest {

    private static final String VALID_TOKEN = "valid-token";
    private static final String EXPIRED_TOKEN = "expired-token";
    private static final String NON_EXISTENT_TOKEN = "non-existent-token";

    @Mock
    private TutorInvitationRepository repository;

    @Spy
    private Clock clock = Clock.fixed(
            Instant.parse("2026-07-04T12:00:00Z"),
            ZoneId.of("UTC")
    );

    @InjectMocks
    private GetTutorInvitation useCase;

    private final TutorInvitationTokenService tokenService = new TutorInvitationTokenService();

    @Test
    void executeShouldReturnPendingInvitationWhenNotExpired() {
        TutorInvitation invitation = givenPendingInvitation(
                VALID_TOKEN,
                LocalDateTime.of(2026, 7, 4, 13, 0)
        );

        GetTutorInvitationResponse response = whenExecute(VALID_TOKEN);

        thenResponseMatchesInvitation(response, invitation);
        thenInvitationStatusRemains(TutorInvitationStatus.PENDING, response);
        thenInvitationWasNotSaved(invitation);
    }

    @Test
    void executeShouldExpirePendingInvitationWhenExpired() {
        TutorInvitation invitation = givenPendingInvitation(
                EXPIRED_TOKEN,
                LocalDateTime.of(2026, 7, 4, 11, 0)
        );

        GetTutorInvitationResponse response = whenExecute(EXPIRED_TOKEN);

        thenResponseMatchesInvitation(response, invitation);
        thenInvitationStatusIsChangedTo(TutorInvitationStatus.EXPIRED, response);
        thenInvitationWasSaved(invitation);
    }

    @Test
    void executeShouldFailWhenTokenDoesNotExist() {
        givenNoInvitationExists(NON_EXISTENT_TOKEN);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> whenExecute(NON_EXISTENT_TOKEN));

        assertEquals("No se encontro la invitacion", exception.getMessage());
    }

    private TutorInvitation givenPendingInvitation(String token, LocalDateTime expiresAt) {
        TutorInvitation invitation = new TutorInvitation();
        invitation.setStatus(TutorInvitationStatus.PENDING);
        invitation.setExpiresAt(expiresAt);
        
        when(repository.findByTokenHash(tokenService.hash(token)))
                .thenReturn(Optional.of(invitation));
        return invitation;
    }

    private void givenNoInvitationExists(String token) {
        when(repository.findByTokenHash(tokenService.hash(token)))
                .thenReturn(Optional.empty());
    }

    private GetTutorInvitationResponse whenExecute(String token) {
        return useCase.execute(token);
    }

    private void thenResponseMatchesInvitation(GetTutorInvitationResponse response, TutorInvitation invitation) {
        assertEquals(invitation.getId(), response.id());
    }

    private void thenInvitationStatusRemains(TutorInvitationStatus status, GetTutorInvitationResponse response) {
        assertEquals(status, response.status());
    }

    private void thenInvitationStatusIsChangedTo(TutorInvitationStatus status, GetTutorInvitationResponse response) {
        assertEquals(status, response.status());
    }

    private void thenInvitationWasNotSaved(TutorInvitation invitation) {
        verify(repository, never()).save(invitation);
    }

    private void thenInvitationWasSaved(TutorInvitation invitation) {
        verify(repository).save(invitation);
    }
}
