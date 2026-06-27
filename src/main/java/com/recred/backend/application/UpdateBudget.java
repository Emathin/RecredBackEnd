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
    public UpdateBudgetResponse execute(UpdateBudgetRequest request) {
        UUID budgetId = request.budgetId();
        PresupuestoAlumno presupuesto = repository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException
                        ("Presupuesto no encontrado con ID: " + budgetId));

        presupuesto.setMontoLimiteGeneral(request.limitAmount());
        presupuesto.setPeriodo(request.period());
        presupuesto.setFechaInicio(request.startDate());

        if (request.categoryRules() != null && !request.categoryRules().isEmpty()) {
            List<ReglaPresupuestoCategoria> nuevasReglas =
                    request.categoryRules().stream().map(rule -> {
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

        PresupuestoAlumno saved = repository.save(presupuesto);
        return mapToResponse(saved);
    }

    private UpdateBudgetResponse mapToResponse(PresupuestoAlumno domain) {
        List<UpdateBudgetResponse.ReglaPresupuestoCategoriaResponse> rules = null;
        if (domain.getReglasCategoria() != null) {
            rules = domain.getReglasCategoria().stream().map(rule -> {
                UpdateBudgetResponse.CategoriaProductoResponse catResponse = null;
                if (rule.getCategoria() != null) {
                    catResponse = new UpdateBudgetResponse.CategoriaProductoResponse(
                            rule.getCategoria().getId(),
                            rule.getCategoria().getDescripcion(),
                            rule.getCategoria().getActivo()
                    );
                }
                return new UpdateBudgetResponse.ReglaPresupuestoCategoriaResponse(
                        rule.getId(),
                        catResponse,
                        rule.getPorcentajeLimite(),
                        rule.getMontoLimiteCalculado(),
                        rule.isActivo()
                );
            }).collect(Collectors.toList());
        }

        return new UpdateBudgetResponse(
                domain.getId(),
                domain.getAlumnoId(),
                domain.getCreadoPorUsuarioId(),
                domain.getMontoLimiteGeneral(),
                domain.getPeriodo(),
                domain.getFechaInicio(),
                domain.isActivo(),
                rules,
                domain.getMontoLimite()
        );
    }
}