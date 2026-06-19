package com.recred.backend.application;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.ReglaPresupuestoCategoria;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

public class CreateBudget {

    private final PresupuestoRepository repository;

    public CreateBudget(PresupuestoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PresupuestoAlumno execute(BudgetCommand command) {
        List<ReglaPresupuestoCategoria> rules = null;

        if (command.categoryRules() != null && !command.categoryRules().isEmpty()) {
            rules = command.categoryRules().stream().map(rule -> {
                CategoriaProducto category = new CategoriaProducto();
                category.setId(rule.categoryId());

                return new ReglaPresupuestoCategoria(
                        null,
                        category,
                        rule.porcentajeLimite(),
                        null,
                        true);
            }).collect(Collectors.toList());
        }

        PresupuestoAlumno budget = new PresupuestoAlumno(
                null,
                command.studentId(),
                command.creadorId(),
                command.limitAmount(),
                command.period(),
                command.startDate(),
                true,
                rules);

        budget.validarPorcentajes();
        budget.recalcularMontosReglas();

        return repository.save(budget);
    }
}
