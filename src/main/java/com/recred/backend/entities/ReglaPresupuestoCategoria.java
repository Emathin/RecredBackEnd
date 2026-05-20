package com.recred.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "reglas_presupuesto_categoria",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"presupuesto_id", "categoria_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ReglaPresupuestoCategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "presupuesto_id", nullable = false)
    private PresupuestoAlumno presupuesto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaProducto categoria;

    @Column(name = "porcentaje_limite", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeLimite;

    @Column(name = "monto_limite_calculado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoLimiteCalculado;

    @Column(nullable = false)
    private Boolean activo = true;
}