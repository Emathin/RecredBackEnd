package com.recred.backend.application;

import com.recred.backend.domain.models.TutorInvitation;

import java.util.UUID;

public record PrepareTutorInvitationAccountResponse(
        UUID invitationId,
        UUID schoolId,
        String schoolName,
        String email,
        String result
) {

    public static PrepareTutorInvitationAccountResponse fromDomain(
            TutorInvitation invitation,
            String result) {
        return new PrepareTutorInvitationAccountResponse(
                invitation.getId(),
                invitation.getSchool() != null ? invitation.getSchool().getId() : null,
                invitation.getSchool() != null ? invitation.getSchool().getNombre() : null,
                invitation.getEmail(),
                result);
    }
}
