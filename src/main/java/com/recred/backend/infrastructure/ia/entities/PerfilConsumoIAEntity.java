package com.recred.backend.infrastructure.ia.entities;

import com.recred.backend.catalogo.infrastructure.entities.ClasificacionTipoEntity;
import com.recred.backend.catalogo.infrastructure.entities.ProductoEntity;
import com.recred.backend.usuarios.infrastructure.entities.AlumnoEntity;
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
public class PerfilConsumoIAEntity {

    @Id
    @GeneratedValue(strategy = UUID)
    private UUID id;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "alumno_id", nullable = false, unique = true)
    private AlumnoEntity alumnoEntity;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "categoria_favorita_id")
    private ClasificacionTipoEntity categoriaFavorita;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "producto_favorito_id")
    private ProductoEntity productoEntityFavorito;

    @Column(name = "promedio_gasto_diario", precision = 12, scale = 2)
    private BigDecimal promedioGastoDiario;

    @Column(name = "score_saludable")
    private Integer scoreSaludable;

    @JdbcTypeCode(JSON)
    @Column(name = "datos_resumen", columnDefinition = "jsonb")
    private Map<String, Object> datosResumen;

    @Column(name = "fecha_ultima_evaluacion")
    private LocalDateTime fechaUltimaEvaluacion;
}
