package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.TutorSchool;
import com.recred.backend.domain.models.TutorSchoolStatus;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tutores_colegios",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tutores_colegios_tutor_colegio",
                columnNames = {"tutor_id", "colegio_id"}))
@Getter
@Setter
@NoArgsConstructor
public class TutorSchoolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private TutorEntity tutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colegio_id", nullable = false)
    private ColegioEntity school;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private TutorSchoolStatus status = TutorSchoolStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime updatedAt;

    public TutorSchool toDomain() {
        TutorSchool tutorSchool = new TutorSchool();
        tutorSchool.setId(id);
        tutorSchool.setTutorId(tutor != null ? tutor.getId() : null);
        tutorSchool.setSchool(school != null ? school.toDomain() : null);
        tutorSchool.setStatus(status);
        tutorSchool.setCreatedAt(createdAt);
        tutorSchool.setUpdatedAt(updatedAt);
        return tutorSchool;
    }
}
