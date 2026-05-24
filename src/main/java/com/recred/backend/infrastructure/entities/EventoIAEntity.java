package com.recred.backend.infrastructure.entities;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "eventos_ia")
@Getter
@Setter
@NoArgsConstructor
public class EventoIAEntity {

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
    @JoinColumn(name = "buffet_id")
    private BuffetEntity buffetEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private ProductoEntity productoEntity;

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