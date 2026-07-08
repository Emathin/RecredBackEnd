package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class TutorInvitation {

    private UUID id;
    private Colegio school;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String tokenHash;
    private TutorInvitationStatus status = TutorInvitationStatus.PENDING;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;

    public boolean isPendingAndExpired(LocalDateTime now) {
        return this.status == TutorInvitationStatus.PENDING && this.expiresAt.isBefore(now);
    }

    public void markAsExpired() {
        this.status = TutorInvitationStatus.EXPIRED;
    }
    public void setupForInvitation(Colegio school, String email, String firstName, 
                                   String lastName, String phone, String tokenHash, 
                                   LocalDateTime expiresAt) {
        this.school = school;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.tokenHash = tokenHash;
        this.status = TutorInvitationStatus.PENDING;
        this.expiresAt = expiresAt;
        this.usedAt = null;
    }
}
