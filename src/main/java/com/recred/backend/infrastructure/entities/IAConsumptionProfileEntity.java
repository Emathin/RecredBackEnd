package com.recred.backend.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.UUID;
import static org.hibernate.type.SqlTypes.JSON;

@Entity
@Table(name = "perfiles_consumo_ia")
@Getter
@Setter
@NoArgsConstructor
public class IAConsumptionProfileEntity {

    @Id
    @GeneratedValue(strategy = UUID)
    private UUID id;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "alumno_id", nullable = false, unique = true)
    private StudentEntity studentEntity;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "categoria_favorita_id")
    private TypeClassificationEntity favouriteCategory;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "producto_favorito_id")
    private ProductEntity productEntityFavorito;

    @Column(name = "promedio_gasto_diario", precision = 12, scale = 2)
    private BigDecimal averageDailySpending;

    @Column(name = "score_saludable")
    private Integer healthScore;

    @JdbcTypeCode(JSON)
    @Column(name = "datos_resumen", columnDefinition = "jsonb")
    private Map<String, Object> summaryData;

    @Column(name = "fecha_ultima_evaluacion")
    private LocalDateTime lastEvaluationDate;
}
