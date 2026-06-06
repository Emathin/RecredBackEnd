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
        List<ReglaPresupuestoCategoria> reglas = null;

        if (command.categoryRules() != null && !command.categoryRules().isEmpty()) {
            reglas = command.categoryRules().stream().map(rule -> {
                CategoriaProducto categoria = new CategoriaProducto();
                categoria.setId(rule.categoryId());

                return new ReglaPresupuestoCategoria(
                        null,
                        categoria,
                        rule.porcentajeLimite(),
                        null,
                        true);
            }).collect(Collectors.toList());
        }

        PresupuestoAlumno presupuesto = new PresupuestoAlumno(
                null,
                command.studentId(),
                command.creadorId(), // <- PASAMOS EL DATO AL DOMINIO ACÁ (Modificá tu BudgetCommand para incluirlo)
                command.limitAmount(),
                command.period(),
                command.startDate(),
                true,
                reglas);

        presupuesto.validarPorcentajes();
        presupuesto.recalcularMontosReglas();

        return repository.save(presupuesto);
    }
}