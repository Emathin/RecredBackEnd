package com.recred.backend.usuarios.infrastructure.entities;

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
@Table(name = "tutores")
@Getter
@Setter
@NoArgsConstructor
public class TutorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private UsuarioEntity usuario;

    @Column(nullable = false)
    private Boolean activa = true;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", updatable = false)
    private LocalDateTime actualizadoEn;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "tutores_alumnos",
            joinColumns = @JoinColumn(name = "tutor_id"),
            inverseJoinColumns = @JoinColumn(name = "alumno_id")
    )
    private Set<AlumnoEntity> hijos = new HashSet<>();

    public void addHijo(AlumnoEntity alumnoEntity) {
        this.hijos.add(alumnoEntity);
        alumnoEntity.getTutores().add(this);
    }

    public void removeHijo(AlumnoEntity alumnoEntity) {
        this.hijos.remove(alumnoEntity);
        alumnoEntity.getTutores().remove(this);
    }
}
