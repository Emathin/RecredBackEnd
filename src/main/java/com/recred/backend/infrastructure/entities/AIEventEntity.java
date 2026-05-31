package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.AIEventType;
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
public class AIEventEntity {

    @Id
    @GeneratedValue(strategy = UUID)
    private UUID id;

    @Enumerated(STRING)
    @Column(name = "tipo", nullable = false)
    private AIEventType type;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "alumno_id")
    private StudentEntity studentEntity;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "buffet_id")
    private BuffetEntity buffetEntity;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "producto_id")
    private ProductEntity productEntity;

    @Column(name = "origen_entrada", columnDefinition = "TEXT")
    private String inputOrigin;

    @JdbcTypeCode(JSON)
    @Column(name = "datos_entrada", columnDefinition = "jsonb")
    private Map<String, Object> inputData;

    @JdbcTypeCode(JSON)
    @Column(name = "datos_resultado", columnDefinition = "jsonb")
    private Map<String, Object> resultData;

    @Column(name = "confianza", precision = 5, scale = 4)
    private BigDecimal confidence;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime createdAt;
}
