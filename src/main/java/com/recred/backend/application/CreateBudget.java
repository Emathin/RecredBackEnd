package com.recred.backend.application;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.ReglaPresupuestoCategoria;
import com.recred.backend.domain.repositories.PresupuestoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

public class CreateBudget {

    private final PresupuestoRepository repository;

    public CreateBudget(PresupuestoRepository repository) {
        this.repository = repository;
    }

    public PresupuestoAlumno execute(BudgetCommand command) {
        List<ReglaPresupuestoCategoria> reglas = null;
        if (command.categoryRules() != null && !command.categoryRules().isEmpty()) {
            reglas = command.categoryRules().stream().map(rule -> {
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

        PresupuestoAlumno presupuesto = new PresupuestoAlumno(
                null,
                command.studentId(),
                command.limitAmount(),
                command.period(),
                command.startDate(),
                true,
                reglas
        );

        return repository.save(presupuesto);
    }
}