package com.recred.backend.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "eventos_ia")
@Getter
@Setter
@NoArgsConstructor
public class EventoIA {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEventoIA tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id")
    private Alumno alumno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kiosco_id")
    private Kiosco kiosco;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "origen_entrada", columnDefinition = "TEXT")
    private String origenEntrada;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_entrada", columnDefinition = "jsonb")
    private Map<String, Object> datosEntrada;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_resultado", columnDefinition = "jsonb")
    private Map<String, Object> datosResultado;

    @Column(precision = 5, scale = 4)
    private BigDecimal confianza;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;
}