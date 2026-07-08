package com.recred.backend.application;

import com.recred.backend.domain.models.TutorInvitation;
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

    public GetTutorInvitationResponse execute(String token) {
        TutorInvitation invitation = findInvitationOrThrow(token);

        expireIfApplicable(invitation);

        return GetTutorInvitationResponse.fromDomain(invitation);
    }

    private TutorInvitation findInvitationOrThrow(String token) {
        return invitationRepository.findByTokenHash(tokenService.hash(token))
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la invitacion"));
    }

    private void expireIfApplicable(TutorInvitation invitation) {
        if (invitation.isPendingAndExpired(LocalDateTime.now(clock))) {
            invitation.markAsExpired();
            invitationRepository.save(invitation);
        }
    }
}
