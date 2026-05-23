package com.recred.backend.dominio.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "perfiles_consumo_ia")
@Getter
@Setter
@NoArgsConstructor
public class PerfilConsumoIAEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false, unique = true)
    private AlumnoEntity alumnoEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_favorita_id")
    private ClasificacionTipoEntity categoriaFavorita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_favorito_id")
    private ProductoEntity productoEntityFavorito;

    @Column(name = "promedio_gasto_diario", precision = 12, scale = 2)
    private BigDecimal promedioGastoDiario;

    @Column(name = "score_saludable")
    private Integer scoreSaludable;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_resumen", columnDefinition = "jsonb")
    private Map<String, Object> datosResumen;

    @Column(name = "fecha_ultima_evaluacion")
    private LocalDateTime fechaUltimaEvaluacion;
}