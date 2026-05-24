package com.recred.backend.infrastructure.entities;

import com.recred.backend.dominio.models.RolUsuario;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colegio_id")
    private ColegioEntity colegioEntity;

    @Column(name = "cognito_sub", unique = true, nullable = false)
    private String idCognito;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(nullable = false)
    private String email;

    private String telefono;

    private String dni;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolUsuario rol;

    @Column(nullable = false)
    private Boolean autenticadoConCognito = false;

    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", updatable = false)
    private LocalDateTime actualizadoEn;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Alumno alumno;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private TutorEntity tutor;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private KiosqueroEntity kiosquero;

    public void setKiosqueroEntity(KiosqueroEntity kiosqueroEntity) {
        this.kiosquero = kiosqueroEntity;
        if (kiosqueroEntity != null) {
            kiosqueroEntity.setUsuario(this);
        }
    }

    public void setAlumnoEntity(Alumno alumno) {
        this.alumno = alumno;
        if (alumno != null) {
            alumno.setUsuario(this);
        }
    }

    public void setTutorEntity(TutorEntity tutorEntity) {
        this.tutor = tutorEntity;
        if (tutorEntity != null) {
            tutorEntity.setUsuario(this);
        }
    }
}
