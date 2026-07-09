package com.recred.backend.infrastructure.web;

import com.recred.backend.application.AcceptTutorInvitationResponse;
import com.recred.backend.domain.models.TutorInvitation;

import java.time.LocalDateTime;
import java.util.UUID;

public record TutorInvitationResponse(
        UUID id,
        UUID schoolId,
        String schoolName,
        String email,
        String firstName,
        String lastName,
        String phone,
        String status,
        LocalDateTime expiresAt,
        String invitationLink,
        String result
) {

    public static TutorInvitationResponse fromDomain(
            TutorInvitation invitation,
            String invitationLink,
            String result) {
        if (invitation == null) {
            return new TutorInvitationResponse(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    invitationLink,
                    result);
        }
        return new TutorInvitationResponse(
                invitation.getId(),
                invitation.getSchool() != null ? invitation.getSchool().getId() : null,
                invitation.getSchool() != null ? invitation.getSchool().getNombre() : null,
                invitation.getEmail(),
                invitation.getFirstName(),
                invitation.getLastName(),
                invitation.getPhone(),
                invitation.getStatus() != null ? invitation.getStatus().name() : null,
                invitation.getExpiresAt(),
                invitationLink,
                result);
    }

    public static TutorInvitationResponse fromApplication(
            AcceptTutorInvitationResponse invitation,
            String invitationLink,
            String result) {
        if (invitation == null) {
            return new TutorInvitationResponse(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    invitationLink,
                    result);
        }
        return new TutorInvitationResponse(
                invitation.id(),
                invitation.schoolId(),
                invitation.schoolName(),
                invitation.email(),
                invitation.firstName(),
                invitation.lastName(),
                invitation.phone(),
                invitation.status() != null ? invitation.status().name() : null,
                invitation.expiresAt(),
                invitationLink,
                result);
    }

    public static TutorInvitationResponse fromApplication(
            com.recred.backend.application.GetTutorInvitationResponse invitation,
            String invitationLink,
            String result) {
        if (invitation == null) {
            return new TutorInvitationResponse(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    invitationLink,
                    result);
        }
        return new TutorInvitationResponse(
                invitation.id(),
                invitation.schoolId(),
                invitation.schoolName(),
                invitation.email(),
                invitation.firstName(),
                invitation.lastName(),
                invitation.phone(),
                invitation.status() != null ? invitation.status().name() : null,
                invitation.expiresAt(),
                invitationLink,
                result);
    }
}
