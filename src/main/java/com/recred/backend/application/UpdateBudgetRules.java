package com.recred.backend.application;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.ReglaPresupuestoCategoria;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class UpdateBudgetRules {

    private final PresupuestoRepository repository;

    public UpdateBudgetRules(PresupuestoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PresupuestoAlumno execute(UUID budgetId, UpdateBudgetRulesCommand command) {
        PresupuestoAlumno presupuesto = repository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException
                        ("Presupuesto no encontrado con ID: " + budgetId));

        List<ReglaPresupuestoCategoria> nuevasReglas = new ArrayList<>();

        if (command.rules() != null && !command.rules().isEmpty()) {
            nuevasReglas = command.rules().stream().map(rule -> {
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
        }

        presupuesto.setReglasCategoria(nuevasReglas);

        presupuesto.validarPorcentajes();
        presupuesto.recalcularMontosReglas();

        return repository.save(presupuesto);
    }
}