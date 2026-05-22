package com.recred.backend.comunicacion.infrastructure.entities;

import com.recred.backend.ia.infrastructure.entities.SugerenciaIAEntity;
import com.recred.backend.usuarios.infrastructure.entities.AlumnoEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
public class AlumnoLogroEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    private AlumnoEntity alumnoEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logro_id", nullable = false)
    private LogroEntity logroEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origen_sugerencia_ia_id")
    private SugerenciaIAEntity origenSugerenciaIaEntity;

    @Column(name = "fecha_obtencion", nullable = false)
    private LocalDateTime fechaObtencion = LocalDateTime.now();
}