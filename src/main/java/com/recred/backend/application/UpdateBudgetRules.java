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
    public UpdateBudgetRulesResponse execute(UpdateBudgetRulesRequest request) {
        PresupuestoAlumno budget = findBudget(request.budgetId());
        updateCategoryRules(budget, request.rules());

        return responseFrom(saveBudget(budget));
    }

    private PresupuestoAlumno findBudget(UUID budgetId) {
        return repository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException(
                        "Presupuesto no encontrado con ID: " + budgetId));
    }

    private List<ReglaPresupuestoCategoria> mapCategoryRules(
            List<UpdateBudgetRulesRequest.CategoryRuleRequest> rules) {
        if (rules == null || rules.isEmpty()) {
            return new ArrayList<>();
        }
        return rules.stream()
                .map(this::categoryRuleFrom)
                .collect(Collectors.toList());
    }

    private ReglaPresupuestoCategoria categoryRuleFrom(
            UpdateBudgetRulesRequest.CategoryRuleRequest rule) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(rule.categoryId());

        return new ReglaPresupuestoCategoria(
                null,
                category,
                rule.porcentajeLimite(),
                null,
                true
        );
    }

    private void updateCategoryRules(
            PresupuestoAlumno budget,
            List<UpdateBudgetRulesRequest.CategoryRuleRequest> rules) {
        budget.setReglasCategoria(mapCategoryRules(rules));
        validateAndRecalculateBudget(budget);
    }

    private void validateAndRecalculateBudget(PresupuestoAlumno budget) {
        budget.validarPorcentajes();
        budget.recalcularMontosReglas();
    }

    private PresupuestoAlumno saveBudget(PresupuestoAlumno budget) {
        return repository.save(budget);
    }

    private UpdateBudgetRulesResponse responseFrom(PresupuestoAlumno domain) {
        List<UpdateBudgetRulesResponse.ReglaPresupuestoCategoriaResponse> rules = null;
        if (domain.getReglasCategoria() != null) {
            rules = domain.getReglasCategoria().stream()
                    .map(this::categoryRuleResponseFrom)
                    .collect(Collectors.toList());
        }

        return new UpdateBudgetRulesResponse(
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

    private UpdateBudgetRulesResponse.ReglaPresupuestoCategoriaResponse categoryRuleResponseFrom(
            ReglaPresupuestoCategoria rule) {
        return new UpdateBudgetRulesResponse.ReglaPresupuestoCategoriaResponse(
                rule.getId(),
                categoryResponseFrom(rule.getCategoria()),
                rule.getPorcentajeLimite(),
                rule.getMontoLimiteCalculado(),
                rule.isActivo()
        );
    }

    private UpdateBudgetRulesResponse.CategoriaProductoResponse categoryResponseFrom(
            CategoriaProducto category) {
        if (category == null) {
            return null;
        }
        return new UpdateBudgetRulesResponse.CategoriaProductoResponse(
                category.getId(),
                category.getDescripcion(),
                category.getActivo()
        );
    }
}
