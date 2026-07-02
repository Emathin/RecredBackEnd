package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Tutor {

    private UUID id;
    private Usuario usuario;
    private Boolean activa = true;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
    private String dashboardConfig;
    private Set<Alumno> hijos = new HashSet<>();

    public void addHijo(Alumno alumno) {
        if (alumno != null) {
            this.hijos.add(alumno);
            alumno.getTutores().add(this);
        }
    }

    public void removeHijo(Alumno alumno) {
        if (alumno != null) {
            this.hijos.remove(alumno);
            alumno.getTutores().remove(this);
        }
    }

    public void validatePurchaseFor(UUID studentId) {
        boolean isOwnChild = this.hijos.stream()
                .anyMatch(child -> child.getId().equals(studentId));
        if (!isOwnChild) {
            throw new SecurityException(
                    "No autorizado para comprar a nombre de este alumno");
        }
    }
}