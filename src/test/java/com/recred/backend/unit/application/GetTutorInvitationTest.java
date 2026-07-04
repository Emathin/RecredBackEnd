package com.recred.backend.unit.application;

import com.recred.backend.application.GetTutorInvitation;
import com.recred.backend.application.TutorInvitationTokenService;
import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;
import com.recred.backend.domain.repositories.TutorInvitationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetTutorInvitationTest {

    private final TutorInvitationRepository repository =
            mock(TutorInvitationRepository.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-07-04T12:00:00Z"),
            ZoneId.of("UTC")
    );
    private final GetTutorInvitation useCase = new GetTutorInvitation(repository, clock);
    private final TutorInvitationTokenService tokenService = new TutorInvitationTokenService();

    @Test
    void executeShouldReturnPendingInvitationWhenNotExpired() {
        String token = "token-valido";
        TutorInvitation invitation = invitation(
                TutorInvitationStatus.PENDING,
                LocalDateTime.of(2026, 7, 4, 13, 0)
        );
        when(repository.findByTokenHash(tokenService.hash(token)))
                .thenReturn(Optional.of(invitation));

        TutorInvitation result = useCase.execute(token);

        assertSame(invitation, result);
        assertEquals(TutorInvitationStatus.PENDING, result.getStatus());
        verify(repository, never()).save(invitation);
    }

    @Test
    void executeShouldExpirePendingInvitationWhenExpired() {
        String token = "token-vencido";
        TutorInvitation invitation = invitation(
                TutorInvitationStatus.PENDING,
                LocalDateTime.of(2026, 7, 4, 11, 0)
        );
        when(repository.findByTokenHash(tokenService.hash(token)))
                .thenReturn(Optional.of(invitation));
        when(repository.save(invitation)).thenReturn(invitation);

        TutorInvitation result = useCase.execute(token);

        assertSame(invitation, result);
        assertEquals(TutorInvitationStatus.EXPIRED, result.getStatus());
        verify(repository).save(invitation);
    }

    @Test
    void executeShouldFailWhenTokenDoesNotExist() {
        String token = "token-inexistente";
        when(repository.findByTokenHash(tokenService.hash(token)))
                .thenReturn(Optional.empty());

        Executable execution = () -> useCase.execute(token);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, execution);
        assertEquals("No se encontro la invitacion", exception.getMessage());
    }

    private TutorInvitation invitation(
            TutorInvitationStatus status,
            LocalDateTime expiresAt
    ) {
        TutorInvitation invitation = new TutorInvitation();
        invitation.setStatus(status);
        invitation.setExpiresAt(expiresAt);
        return invitation;
    }
}
