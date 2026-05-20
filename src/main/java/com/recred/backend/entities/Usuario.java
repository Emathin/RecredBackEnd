package com.recred.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_cognito", unique = true, nullable = false)
    private String idCognito;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(nullable = false)
    private String email;

    private String telefono;
    private String dni;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colegio_id")
    private Colegio colegio;

    @Column(nullable = false)
    private Boolean activo = true;

    // Relaciones bidireccionales 1 a 1
    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Alumno alumno;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Tutor tutor;

    // Agregar dentro de Usuario.java
    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Kiosquero kiosquero;

    public void setKiosquero(Kiosquero kiosquero) {
        this.kiosquero = kiosquero;
        if (kiosquero != null) {
            kiosquero.setUsuario(this);
        }
    }

    // Métodos de conveniencia para sincronizar la relación bidireccional
    public void setAlumno(Alumno alumno) {
        this.alumno = alumno;
        if (alumno != null) {
            alumno.setUsuario(this);
        }
    }

    public void setTutor(Tutor tutor) {
        this.tutor = tutor;
        if (tutor != null) {
            tutor.setUsuario(this);
        }
    }
}
