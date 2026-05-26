package com.recred.backend.application;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.ReglaPresupuestoCategoria;
import com.recred.backend.domain.repositories.PresupuestoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class UpdateBudget {

    private final PresupuestoRepository repository;

    public UpdateBudget(PresupuestoRepository repository) {
        this.repository = repository;
    }

    public PresupuestoAlumno execute(UUID budgetId, BudgetCommand command) {
        PresupuestoAlumno presupuesto = repository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Presupuesto " +
                        "no encontrado con ID: " + budgetId));

        presupuesto.setMontoLimiteGeneral(command.limitAmount());
        presupuesto.setPeriodo(command.period());
        presupuesto.setFechaInicio(command.startDate());

        List<ReglaPresupuestoCategoria> nuevasReglas = null;
        if (command.categoryRules() != null && !command.categoryRules().isEmpty()) {
            nuevasReglas = command.categoryRules().stream().map(rule -> {
                CategoriaProducto categoria = new CategoriaProducto();
                categoria.setId(rule.categoryId());

                BigDecimal montoCalculado = command.limitAmount()
                        .multiply(rule.porcentajeLimite())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                return new ReglaPresupuestoCategoria(
                        null,
                        categoria,
                        rule.porcentajeLimite(),
                        montoCalculado,
                        true
                );
            }).collect(Collectors.toList());
        }

        presupuesto.setReglasCategoria(nuevasReglas);

        return repository.save(presupuesto);
    }
}