package com.recred.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tutores")
@Getter
@Setter
@NoArgsConstructor
public class Tutor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "tutor_alumno",
            joinColumns = @JoinColumn(name = "tutor_id"),
            inverseJoinColumns = @JoinColumn(name = "alumno_id")
    )
    private Set<Alumno> hijos = new HashSet<>();

    // Métodos de conveniencia para mantener la colección sincronizada
    public void addHijo(Alumno alumno) {
        this.hijos.add(alumno);
        alumno.getTutores().add(this);
    }

    public void removeHijo(Alumno alumno) {
        this.hijos.remove(alumno);
        alumno.getTutores().remove(this);
    }
}
