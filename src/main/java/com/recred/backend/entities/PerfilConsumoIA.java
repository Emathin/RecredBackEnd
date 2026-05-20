package com.recred.backend.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "perfiles_consumo_ia")
@Getter
@Setter
@NoArgsConstructor
public class PerfilConsumoIA {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false, unique = true)
    private Alumno alumno;

    // Almacenamiento flexible JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metricas_consumo", columnDefinition = "jsonb")
    private Map<String, Object> metricasConsumo;

    @Column(name = "ultima_actualizacion")
    private LocalDateTime ultimaActualizacion;
}