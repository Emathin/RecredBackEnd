package com.recred.backend.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
public class ReglaPresupuestoCategoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "presupuesto_id", nullable = false)
    private PresupuestoAlumnoEntity presupuesto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaProductoEntity categoria;

    @Column(name = "porcentaje_limite", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeLimite;

    @Column(name = "monto_limite_calculado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoLimiteCalculado;

    @Column(nullable = false)
    private Boolean activo = true;
}