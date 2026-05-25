package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class FranjaHoraria {

    private UUID id;
    private UUID colegioId;
    private String descripcion;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private boolean activo = true;

    public FranjaHoraria(UUID id, UUID colegioId, String descripcion,
                         LocalTime horaInicio, LocalTime horaFin, boolean activo) {
        this.id = id;
        this.colegioId = colegioId;
        this.descripcion = descripcion;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.activo = activo;
    }

    public boolean estaDentroDeLaFranja(LocalDateTime momento) {
        if (momento == null) {
            return false;
        }

        LocalTime tiempo = momento.toLocalTime();

        return !tiempo.isBefore(this.horaInicio) && !tiempo.isAfter(this.horaFin);
    }
}