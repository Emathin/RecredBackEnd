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
        PresupuestoAlumno presupuesto = this.findPresupuesto(request.budgetId());

        List<ReglaPresupuestoCategoria> nuevasReglas = this.mapCategoryRules(request.rules());
        presupuesto.setReglasCategoria(nuevasReglas);

        this.validateAndRecalculateBudget(presupuesto);

        PresupuestoAlumno saved = this.savePresupuesto(presupuesto);
        return mapToResponse(saved);
    }

    private PresupuestoAlumno findPresupuesto(UUID budgetId) {
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
                .map(this::mapToRegla)
                .collect(Collectors.toList());
    }

    private ReglaPresupuestoCategoria mapToRegla(UpdateBudgetRulesRequest.CategoryRuleRequest rule) {
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

    private UpdateBudgetRulesResponse mapToResponse(PresupuestoAlumno domain) {
        List<UpdateBudgetRulesResponse.ReglaPresupuestoCategoriaResponse> rules = null;
        if (domain.getReglasCategoria() != null) {
            rules = domain.getReglasCategoria().stream().map(rule -> {
                UpdateBudgetRulesResponse.CategoriaProductoResponse catResponse = null;
                if (rule.getCategoria() != null) {
                    catResponse = new UpdateBudgetRulesResponse.CategoriaProductoResponse(
                            rule.getCategoria().getId(),
                            rule.getCategoria().getDescripcion(),
                            rule.getCategoria().getActivo()
                    );
                }
                return new UpdateBudgetRulesResponse.ReglaPresupuestoCategoriaResponse(
                        rule.getId(),
                        catResponse,
                        rule.getPorcentajeLimite(),
                        rule.getMontoLimiteCalculado(),
                        rule.isActivo()
                );
            }).collect(Collectors.toList());
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
}