package com.recred.backend.infrastructure.entities;


import com.recred.backend.domain.models.ProductCategory;
import com.recred.backend.domain.models.BudgetCategoryRule;
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
public class BudgetCategoryRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "presupuesto_id", nullable = false)
    private StudentBudgetEntity budget;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private ProductCategoryEntity category;

    @Column(name = "porcentaje_limite", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentageLimit;

    @Column(name = "monto_limite_calculado", nullable = false, precision = 12, scale = 2)
    private BigDecimal calculatedLimitAmount;

    @Column(nullable = false)
    private Boolean active = true;

    public BudgetCategoryRule toDomain() {
        BudgetCategoryRule domain = new BudgetCategoryRule();

        domain.setId(this.id);
        domain.setLimitAverage(this.percentageLimit);
        domain.setCalculatedLimitAmount(this.calculatedLimitAmount);
        domain.setActive(this.active != null ? this.active : false);

        if (this.category != null) {
            ProductCategory catDomain = new ProductCategory();
            catDomain.setId(this.category.getId());

            domain.setCategory(catDomain);
        }

        return domain;
    }
}