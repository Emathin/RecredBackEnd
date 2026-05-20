package com.recred.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "alumnos_logros")
@Getter
@Setter
@NoArgsConstructor
public class AlumnoLogro {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logro_id", nullable = false)
    private Logro logro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origen_sugerencia_ia_id")
    private SugerenciaIA origenSugerenciaIa;

    @Column(name = "fecha_obtencion", nullable = false)
    private LocalDateTime fechaObtencion = LocalDateTime.now();
}