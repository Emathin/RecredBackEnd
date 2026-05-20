package com.recred.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "restricciones_horarias")
@Getter
@Setter
@NoArgsConstructor
public class RestriccionHoraria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "franja_id", nullable = false)
    private FranjaHoraria franjaHoraria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clasificacion_tipo_id", nullable = false)
    private ClasificacionTipo clasificacionTipo;

    @Column(nullable = false)
    private Boolean activa = true;
}