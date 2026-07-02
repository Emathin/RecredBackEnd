package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.SchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationStatus;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "solicitud_registro_colegio")
@Getter
@Setter
@NoArgsConstructor
public class SchoolRegistrationRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nombre_colegio", nullable = false)
    private String schoolName;

    @Column(name = "email_colegio", nullable = false)
    private String schoolEmail;

    @Column(name = "telefono_colegio")
    private String schoolPhone;

    @Column(name = "cue_colegio", nullable = false)
    private String schoolCue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nivel_id")
    private NivelEntity schoolLevel;

    @Column(name = "nombre_director", nullable = false)
    private String directorFirstName;

    @Column(name = "apellido_director", nullable = false)
    private String directorLastName;

    @Column(name = "email_director", nullable = false)
    private String directorEmail;

    @Column(name = "telefono_director")
    private String directorPhone;

    @Column(name = "dni_director")
    private String directorDni;

    @Column(name = "username_director")
    private String directorUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private SchoolRegistrationStatus status;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime updatedAt;

    public SchoolRegistrationRequest toDomain() {
        SchoolRegistrationRequest req = new SchoolRegistrationRequest();
        req.setId(this.id);
        req.setSchoolName(this.schoolName);
        req.setSchoolEmail(this.schoolEmail);
        req.setSchoolPhone(this.schoolPhone);
        req.setSchoolCue(this.schoolCue);
        if (this.schoolLevel != null) {
            req.setSchoolLevel(this.schoolLevel.toDomain());
        }
        req.setDirectorFirstName(this.directorFirstName);
        req.setDirectorLastName(this.directorLastName);
        req.setDirectorEmail(this.directorEmail);
        req.setDirectorPhone(this.directorPhone);
        req.setDirectorDni(this.directorDni);
        req.setDirectorUsername(this.directorUsername);
        req.setStatus(this.status);
        req.setCreatedAt(this.createdAt);
        req.setUpdatedAt(this.updatedAt);
        return req;
    }

    public static SchoolRegistrationRequestEntity fromDomain(SchoolRegistrationRequest req) {
        if (req == null) {
            return null;
        }
        SchoolRegistrationRequestEntity entity = new SchoolRegistrationRequestEntity();
        entity.setId(req.getId());
        entity.setSchoolName(req.getSchoolName());
        entity.setSchoolEmail(req.getSchoolEmail());
        entity.setSchoolPhone(req.getSchoolPhone());
        entity.setSchoolCue(req.getSchoolCue());
        if (req.getSchoolLevel() != null) {
            NivelEntity nivel = new NivelEntity();
            nivel.setId(req.getSchoolLevel().getId());
            entity.setSchoolLevel(nivel);
        }
        entity.setDirectorFirstName(req.getDirectorFirstName());
        entity.setDirectorLastName(req.getDirectorLastName());
        entity.setDirectorEmail(req.getDirectorEmail());
        entity.setDirectorPhone(req.getDirectorPhone());
        entity.setDirectorDni(req.getDirectorDni());
        entity.setDirectorUsername(req.getDirectorUsername());
        entity.setStatus(req.getStatus());
        entity.setCreatedAt(req.getCreatedAt());
        entity.setUpdatedAt(req.getUpdatedAt());
        return entity;
    }
}
