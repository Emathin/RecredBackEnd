package com.recred.backend.application;

import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;
import com.recred.backend.domain.repositories.TutorInvitationRepository;

import java.time.Clock;
import java.time.LocalDateTime;

public class GetTutorInvitation {

    private final TutorInvitationRepository invitationRepository;
    private final TutorInvitationTokenService tokenService;
    private final Clock clock;

    public GetTutorInvitation(TutorInvitationRepository invitationRepository, Clock clock) {
        this.invitationRepository = invitationRepository;
        this.tokenService = new TutorInvitationTokenService();
        this.clock = clock;
    }

    public TutorInvitation execute(String token) {
        TutorInvitation invitation = invitationRepository.findByTokenHash(tokenService.hash(token))
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la invitacion"));
        if (invitation.getStatus() == TutorInvitationStatus.PENDING
                && invitation.getExpiresAt().isBefore(LocalDateTime.now(clock))) {
            invitation.setStatus(TutorInvitationStatus.EXPIRED);
            return invitationRepository.save(invitation);
        }
        return invitation;
    }
}
