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
        PresupuestoAlumno presupuesto = this.findPresupuesto(request.budgetId());

        this.updateBasicFields(presupuesto, request);
        this.applyCategoryRules(presupuesto, request.categoryRules());
        this.validateAndRecalculateBudget(presupuesto);

        PresupuestoAlumno saved = this.savePresupuesto(presupuesto);
        return mapToResponse(saved);
    }

    private PresupuestoAlumno findPresupuesto(UUID budgetId) {
        return repository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException(
                        "Presupuesto no encontrado con ID: " + budgetId));
    }

    private void updateBasicFields(PresupuestoAlumno presupuesto, UpdateBudgetRequest request) {
        presupuesto.setMontoLimiteGeneral(request.limitAmount());
        presupuesto.setPeriodo(request.period());
        presupuesto.setFechaInicio(request.startDate());
    }

    private void applyCategoryRules(
            PresupuestoAlumno presupuesto,
            List<UpdateBudgetRequest.CategoryRuleRequest> categoryRules) {
        if (categoryRules != null && !categoryRules.isEmpty()) {
            List<ReglaPresupuestoCategoria> nuevasReglas = categoryRules.stream()
                    .map(this::mapToRegla)
                    .collect(Collectors.toList());

            presupuesto.setReglasCategoria(nuevasReglas);
        }
    }

    private ReglaPresupuestoCategoria mapToRegla(UpdateBudgetRequest.CategoryRuleRequest rule) {
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setId(rule.categoryId());

        return new ReglaPresupuestoCategoria(
                null,
                categoria,
                rule.porcentajeLimite(),
                null,
                true
        );
    }

    private void validateAndRecalculateBudget(PresupuestoAlumno presupuesto) {
        presupuesto.validarPorcentajes();
        presupuesto.recalcularMontosReglas();
    }

    private PresupuestoAlumno savePresupuesto(PresupuestoAlumno presupuesto) {
        return repository.save(presupuesto);
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