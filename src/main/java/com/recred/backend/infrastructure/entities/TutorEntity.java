package com.recred.backend.infrastructure.entities;


import com.recred.backend.domain.models.Tutor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tutors")
@Getter
@Setter
@NoArgsConstructor
public class TutorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "actualizado_en", updatable = false)
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "tutores_alumnos",
            joinColumns = @JoinColumn(name = "tutor_id"),
            inverseJoinColumns = @JoinColumn(name = "alumno_id")
    )
    private Set<StudentEntity> children = new HashSet<>();

    public void addHijo(StudentEntity studentEntity) {
        this.children.add(studentEntity);
        studentEntity.getTutors().add(this);
    }

    public void removeHijo(StudentEntity studentEntity) {
        this.children.remove(studentEntity);
        studentEntity.getTutors().remove(this);
    }

    public Tutor toDomain() {
        Tutor tutor = new Tutor();

        tutor.setId(this.id);
        tutor.setActive(this.active);
        tutor.setCreatedAt(this.createdAt);
        tutor.setUpdatedAt(this.updatedAt);

        if (this.children != null && !this.children.isEmpty()) {
            this.children.forEach(hijoEntity -> {
                tutor.getChildren().add(hijoEntity.toDomain());
            });
        }

        return tutor;
    }

    public static TutorEntity fromDomain(Tutor tutor) {
        if (tutor == null) {
            return null;
        }

        TutorEntity entity = new TutorEntity();
        entity.setId(tutor.getId());
        entity.setActive(tutor.getActive());
        entity.setCreatedAt(tutor.getCreatedAt());
        entity.setUpdatedAt(tutor.getUpdatedAt());

        if (tutor.getChildren() != null && !tutor.getChildren().isEmpty()) {
            tutor.getChildren().forEach(alumno -> {
                if (alumno.getId() != null) {
                    StudentEntity alumnoProxy = new StudentEntity();
                    alumnoProxy.setId(alumno.getId());
                    entity.addHijo(alumnoProxy);
                }
            });
        }

        return entity;
    }
}
