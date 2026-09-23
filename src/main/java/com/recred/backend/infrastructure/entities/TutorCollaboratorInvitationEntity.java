package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.TutorCollaboratorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "invitaciones_colaborador")
@Getter
@Setter
@NoArgsConstructor
public class TutorCollaboratorInvitationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tutor_principal_id", nullable = false)
    private UUID tutorPrincipalId;

    @Column(nullable = false)
    private String email;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TutorInvitationStatus estado;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "usada_en")
    private LocalDateTime usadaEn;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "invitaciones_colaborador_alumnos",
            joinColumns = @JoinColumn(name = "invitacion_id")
    )
    @Column(name = "alumno_id")
    private Set<UUID> assignedStudentIds = new HashSet<>();

    public TutorCollaboratorInvitation toDomain() {
        TutorCollaboratorInvitation domain = new TutorCollaboratorInvitation();
        domain.setId(this.id);
        domain.setTutorPrincipalId(this.tutorPrincipalId);
        domain.setEmail(this.email);
        domain.setTokenHash(this.tokenHash);
        domain.setStatus(this.estado);
        domain.setCreadoEn(this.creadoEn);
        domain.setExpiraEn(this.expiraEn);
        domain.setUsadaEn(this.usadaEn);
        if (this.assignedStudentIds != null) {
            domain.setAssignedStudentIds(new HashSet<>(this.assignedStudentIds));
        }
        return domain;
    }

    public static TutorCollaboratorInvitationEntity fromDomain(TutorCollaboratorInvitation domain) {
        if (domain == null) return null;
        TutorCollaboratorInvitationEntity entity = new TutorCollaboratorInvitationEntity();
        entity.setId(domain.getId());
        entity.setTutorPrincipalId(domain.getTutorPrincipalId());
        entity.setEmail(domain.getEmail());
        entity.setTokenHash(domain.getTokenHash());
        entity.setEstado(domain.getStatus());
        entity.setCreadoEn(domain.getCreadoEn());
        entity.setExpiraEn(domain.getExpiraEn());
        entity.setUsadaEn(domain.getUsadaEn());
        if (domain.getAssignedStudentIds() != null) {
            entity.setAssignedStudentIds(new HashSet<>(domain.getAssignedStudentIds()));
        }
        return entity;
    }
}
