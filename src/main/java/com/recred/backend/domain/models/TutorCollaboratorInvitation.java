package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class TutorCollaboratorInvitation {

    private UUID id;
    private UUID tutorPrincipalId;
    private String email;
    private String tokenHash;
    private TutorInvitationStatus status;
    private LocalDateTime creadoEn;
    private LocalDateTime expiraEn;
    private LocalDateTime usadaEn;
    private Set<UUID> assignedStudentIds = new HashSet<>();

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiraEn);
    }
}
