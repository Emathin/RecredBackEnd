package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.models.PeriodBudget;
import com.recred.backend.domain.models.StudentBudget;
import com.recred.backend.domain.models.BudgetCategoryRule;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Table(name = "presupuestos_alumnos")
@Getter
@Setter
@NoArgsConstructor
public class StudentBudgetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    private StudentEntity studentEntity;

    @Enumerated(STRING)
    @Column(nullable = false)
    private BudgetPeriod period;

    @Column(name = "monto_limite", nullable = false, precision = 12, scale = 2)
    private BigDecimal limitAmount;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate startDate;

    @Column(name = "fecha_fin")
    private LocalDate endDate;

    @Column(nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_usuario_id", nullable = false)
    private UserEntity createdByUserEntity;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BudgetCategoryRuleEntity> categoryRules = new ArrayList<>();

    public StudentBudget toDomain() {
        List<BudgetCategoryRule> listaDomain = new ArrayList<>();

        if (this.categoryRules != null) {
            listaDomain = this.categoryRules.stream()
                    .map(BudgetCategoryRuleEntity::toDomain)
                    .collect(Collectors.toList());
        }

        return new StudentBudget(
                this.id,
                this.studentEntity != null ? this.studentEntity.getId() : null,
                this.limitAmount,
                this.period != null ? PeriodBudget.valueOf(this.period.name()) : null,
                this.startDate,
                this.active != null ? this.active : false,
                listaDomain
        );
    }
}