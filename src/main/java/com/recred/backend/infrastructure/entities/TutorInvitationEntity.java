package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invitaciones_tutor")
@Getter
@Setter
@NoArgsConstructor
public class TutorInvitationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colegio_id", nullable = false)
    private ColegioEntity school;

    @Column(nullable = false)
    private String email;

    @Column(name = "nombre")
    private String firstName;

    @Column(name = "apellido")
    private String lastName;

    @Column(name = "telefono")
    private String phone;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private TutorInvitationStatus status = TutorInvitationStatus.PENDING;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "usada_en")
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime createdAt;

    public TutorInvitation toDomain() {
        TutorInvitation invitation = new TutorInvitation();
        invitation.setId(id);
        invitation.setSchool(school != null ? school.toDomain() : null);
        invitation.setEmail(email);
        invitation.setFirstName(firstName);
        invitation.setLastName(lastName);
        invitation.setPhone(phone);
        invitation.setTokenHash(tokenHash);
        invitation.setStatus(status);
        invitation.setExpiresAt(expiresAt);
        invitation.setUsedAt(usedAt);
        invitation.setCreatedAt(createdAt);
        return invitation;
    }
}
