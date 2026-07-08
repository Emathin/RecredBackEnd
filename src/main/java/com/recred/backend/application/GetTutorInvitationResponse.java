package com.recred.backend.application;

import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record GetTutorInvitationResponse(
        UUID id,
        UUID schoolId,
        String schoolName,
        String email,
        String firstName,
        String lastName,
        String phone,
        TutorInvitationStatus status,
        LocalDateTime expiresAt,
        LocalDateTime usedAt,
        LocalDateTime createdAt
) {
    public static GetTutorInvitationResponse fromDomain(TutorInvitation invitation) {
        return new GetTutorInvitationResponse(
                invitation.getId(),
                invitation.getSchool() != null ? invitation.getSchool().getId() : null,
                invitation.getSchool() != null ? invitation.getSchool().getNombre() : null,
                invitation.getEmail(),
                invitation.getFirstName(),
                invitation.getLastName(),
                invitation.getPhone(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getUsedAt(),
                invitation.getCreatedAt()
        );
    }
}
