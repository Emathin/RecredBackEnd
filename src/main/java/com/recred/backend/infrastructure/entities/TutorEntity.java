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

    @Column(name = "dashboard_config", columnDefinition = "TEXT")
    private String dashboardConfig;

    @Column(name = "es_colaborador", nullable = false, columnDefinition = "boolean default false")
    private Boolean esColaborador = false;

    @Column(name = "tutor_principal_id")
    private UUID tutorPrincipalId;

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

    public Tutor toDomain() {
        Tutor tutor = new Tutor();

        tutor.setId(this.id);
        tutor.setActiva(this.activa);
        tutor.setCreadoEn(this.creadoEn);
        tutor.setActualizadoEn(this.actualizadoEn);
        tutor.setDashboardConfig(this.dashboardConfig);
        tutor.setEsColaborador(this.esColaborador != null ? this.esColaborador : false);
        tutor.setTutorPrincipalId(this.tutorPrincipalId);

        if (this.usuario != null) {
            tutor.setUsuario(this.usuario.toDomain());
        }

        if (this.hijos != null && !this.hijos.isEmpty()) {
            this.hijos.forEach(hijoEntity -> {
                tutor.getHijos().add(hijoEntity.toDomain());
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
        entity.setActiva(tutor.getActiva());
        entity.setCreadoEn(tutor.getCreadoEn());
        entity.setActualizadoEn(tutor.getActualizadoEn());
        entity.setDashboardConfig(tutor.getDashboardConfig());
        entity.setEsColaborador(tutor.getEsColaborador() != null 
                ? tutor.getEsColaborador() : false);
        entity.setTutorPrincipalId(tutor.getTutorPrincipalId());

        if (tutor.getHijos() != null && !tutor.getHijos().isEmpty()) {
            tutor.getHijos().forEach(alumno -> {
                if (alumno.getId() != null) {
                    AlumnoEntity alumnoProxy = new AlumnoEntity();
                    alumnoProxy.setId(alumno.getId());
                    entity.addHijo(alumnoProxy);
                }
            });
        }

        return entity;
    }
}
