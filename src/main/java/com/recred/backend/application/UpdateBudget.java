package com.recred.backend.application;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.ReglaPresupuestoCategoria;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class UpdateBudget {

    private final PresupuestoRepository repository;

    public UpdateBudget(PresupuestoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PresupuestoAlumno execute(UUID budgetId, BudgetCommand command) {
        PresupuestoAlumno budget = repository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException
                        ("Presupuesto no encontrado con ID: " + budgetId));

        budget.setMontoLimiteGeneral(command.limitAmount());
        budget.setPeriodo(command.period());
        budget.setFechaInicio(command.startDate());

        if (command.categoryRules() != null && !command.categoryRules().isEmpty()) {
            List<ReglaPresupuestoCategoria> newRules =
                    command.categoryRules().stream().map(rule -> {
                CategoriaProducto category = new CategoriaProducto();
                category.setId(rule.categoryId());

                return new ReglaPresupuestoCategoria(
                        null,
                        category,
                        rule.porcentajeLimite(),
                        null,
                        true
                );
            }).collect(Collectors.toList());

            budget.setReglasCategoria(newRules);
        }

        budget.validarPorcentajes();
        budget.recalcularMontosReglas();

        return repository.save(budget);
    }
}
