package com.recred.backend.application;

import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AcceptTutorInvitationResponse(
        UUID id,
        UUID schoolId,
        String schoolName,
        String email,
        String firstName,
        String lastName,
        String phone,
        TutorInvitationStatus status,
        LocalDateTime expiresAt
) {
    public static AcceptTutorInvitationResponse fromDomain(TutorInvitation invitation) {
        Colegio school = invitation.getSchool();
        return new AcceptTutorInvitationResponse(
                invitation.getId(),
                school == null ? null : school.getId(),
                school == null ? null : school.getNombre(),
                invitation.getEmail(),
                invitation.getFirstName(),
                invitation.getLastName(),
                invitation.getPhone(),
                invitation.getStatus(),
                invitation.getExpiresAt()
        );
    }
}
