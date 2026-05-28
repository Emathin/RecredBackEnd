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
        PresupuestoAlumno presupuesto = repository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException
                        ("Presupuesto no encontrado con ID: " + budgetId));

        presupuesto.setMontoLimiteGeneral(command.limitAmount());
        presupuesto.setPeriodo(command.period());
        presupuesto.setFechaInicio(command.startDate());

        if (command.categoryRules() != null && !command.categoryRules().isEmpty()) {
            List<ReglaPresupuestoCategoria> nuevasReglas =
                    command.categoryRules().stream().map(rule -> {
                CategoriaProducto categoria = new CategoriaProducto();
                categoria.setId(rule.categoryId());

                return new ReglaPresupuestoCategoria(
                        null,
                        categoria,
                        rule.porcentajeLimite(),
                        null,
                        true
                );
            }).collect(Collectors.toList());

            presupuesto.setReglasCategoria(nuevasReglas);
        }

        presupuesto.validarPorcentajes();
        presupuesto.recalcularMontosReglas();

        return repository.save(presupuesto);
    }
}