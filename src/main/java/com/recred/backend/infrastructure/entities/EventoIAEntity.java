package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.TipoEventoIA;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.UUID;
import static org.hibernate.type.SqlTypes.JSON;

@Entity
@Table(name = "eventos_ia")
@Getter
@Setter
@NoArgsConstructor
public class EventoIAEntity {

    @Id
    @GeneratedValue(strategy = UUID)
    private UUID id;

    @Enumerated(STRING)
    @Column(nullable = false)
    private TipoEventoIA tipo;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "alumno_id")
    private AlumnoEntity alumnoEntity;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "buffet_id")
    private BuffetEntity buffetEntity;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "producto_id")
    private ProductoEntity productoEntity;

    @Column(name = "origen_entrada", columnDefinition = "TEXT")
    private String origenEntrada;

    @JdbcTypeCode(JSON)
    @Column(name = "datos_entrada", columnDefinition = "jsonb")
    private Map<String, Object> datosEntrada;

    @JdbcTypeCode(JSON)
    @Column(name = "datos_resultado", columnDefinition = "jsonb")
    private Map<String, Object> datosResultado;

    @Column(precision = 5, scale = 4)
    private BigDecimal confianza;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;
}
