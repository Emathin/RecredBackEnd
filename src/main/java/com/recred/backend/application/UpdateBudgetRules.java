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
        UUID budgetId = request.budgetId();
        PresupuestoAlumno presupuesto = repository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException
                        ("Presupuesto no encontrado con ID: " + budgetId));

        List<ReglaPresupuestoCategoria> nuevasReglas = new ArrayList<>();

        if (request.rules() != null && !request.rules().isEmpty()) {
            nuevasReglas = request.rules().stream().map(rule -> {
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

        PresupuestoAlumno saved = repository.save(presupuesto);
        return mapToResponse(saved);
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