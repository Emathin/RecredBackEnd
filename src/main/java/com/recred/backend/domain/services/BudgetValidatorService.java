package com.recred.backend.domain.services;

import com.recred.backend.domain.exceptions.ExceededBudgetException;
import com.recred.backend.domain.models.StudentBudget;
import com.recred.backend.domain.models.RangoFechas;
import com.recred.backend.domain.models.BudgetCategoryRule;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.BudgetRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class BudgetValidatorService {

    private final BudgetRepository budgetRepository;
    private final PurchaseRepository purchaseRepository;
    private final BudgetPeriodCalculator periodCalculator;

    public BudgetValidatorService(BudgetRepository budgetRepository,
                                  PurchaseRepository purchaseRepository,
                                  BudgetPeriodCalculator periodCalculator) {
        this.budgetRepository = budgetRepository;
        this.purchaseRepository = purchaseRepository;
        this.periodCalculator = periodCalculator;
    }

    public void validateBudget(UUID alumnoId,
                               BigDecimal totalCartAmount,
                               Map<UUID, BigDecimal> amountsByCategory,
                               LocalDateTime purchaseMoment) {

        Optional<StudentBudget> optBudget =
                budgetRepository.findByActiveStudentID(alumnoId);
        if (optBudget.isEmpty()) {
            return;
        }

        StudentBudget budget = optBudget.get();

        if (!budget.isCurrent(purchaseMoment.toLocalDate())) {
            return;
        }

        RangoFechas range = periodCalculator.calculateRange(
                budget.getPeriod(), purchaseMoment);

        BigDecimal gastadoHistorico = purchaseRepository.sumExpensesByStudentInRange(
                alumnoId, range.inicio(), range.fin()
        );

        BigDecimal totalProyectado = gastadoHistorico.add(totalCartAmount);

        if (totalProyectado.compareTo(budget.getLimitAmount()) > 0) {
            BigDecimal disponible = budget.getLimitAmount().subtract(gastadoHistorico);
            throw new ExceededBudgetException(
                    String.format("Presupuesto %s excedido. Disponible: $%s",
                            budget.getPeriod().name().toLowerCase(),
                            disponible.max(BigDecimal.ZERO))
            );
        }

        if (budget.getCategoryRules() != null && !budget.
                getCategoryRules().isEmpty()) {
            for (BudgetCategoryRule regla : budget.getCategoryRules()) {
                UUID catId = regla.getCategory().getId();

                BigDecimal gastoCategoriaEnCarrito = amountsByCategory.
                        getOrDefault(catId, BigDecimal.ZERO);

                if (gastoCategoriaEnCarrito.compareTo(BigDecimal.ZERO) > 0) {

                    BigDecimal gastadoHistoricoCat = purchaseRepository.
                            sumExpensesByCategoryInRange(
                            alumnoId, catId, range.inicio(), range.fin()
                    );

                    BigDecimal proyectadoCat = gastadoHistoricoCat.add(gastoCategoriaEnCarrito);

                    if (proyectadoCat.compareTo(regla.getCalculatedLimitAmount()) > 0) {
                        BigDecimal disponibleCat = regla.getCalculatedLimitAmount().
                                subtract(gastadoHistoricoCat);
                        throw new ExceededBudgetException(
                                String.format("Límite superado para una categoría restringida. " +
                                                "Disponible en categoría: $%s",
                                        disponibleCat.max(BigDecimal.ZERO))
                        );
                    }
                }
            }
        }
    }
}