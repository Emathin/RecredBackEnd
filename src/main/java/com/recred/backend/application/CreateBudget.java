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
    public CreateBudgetResponse execute(CreateBudgetRequest request) {
        List<ReglaPresupuestoCategoria> reglas = this.mapCategoryRules(request.categoryRules());
        PresupuestoAlumno presupuesto = this.buildPresupuesto(request, reglas);

        presupuesto.validarPorcentajes();
        presupuesto.recalcularMontosReglas();

        PresupuestoAlumno saved = repository.save(presupuesto);
        return mapToResponse(saved);
    }

    private List<ReglaPresupuestoCategoria> mapCategoryRules(
            List<CreateBudgetRequest.CategoryRuleRequest> categoryRules) {
        if (categoryRules == null || categoryRules.isEmpty()) {
            return null;
        }
        return categoryRules.stream().map(this::mapToCategoryRule).collect(Collectors.toList());
    }

    private ReglaPresupuestoCategoria mapToCategoryRule(
            CreateBudgetRequest.CategoryRuleRequest rule) {
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setId(rule.categoryId());

        return new ReglaPresupuestoCategoria(
                null,
                categoria,
                rule.porcentajeLimite(),
                null,
                true);
    }

    private PresupuestoAlumno buildPresupuesto(
            CreateBudgetRequest request,
            List<ReglaPresupuestoCategoria> reglas) {
        return new PresupuestoAlumno(
                null,
                request.studentId(),
                request.creadorId(),
                request.limitAmount(),
                request.period(),
                request.startDate(),
                true,
                reglas);
    }

    private CreateBudgetResponse mapToResponse(PresupuestoAlumno domain) {
        List<CreateBudgetResponse.ReglaPresupuestoCategoriaResponse> rules = null;
        if (domain.getReglasCategoria() != null) {
            rules = domain.getReglasCategoria().stream().map(rule -> {
                CreateBudgetResponse.CategoriaProductoResponse catResponse = null;
                if (rule.getCategoria() != null) {
                    catResponse = new CreateBudgetResponse.CategoriaProductoResponse(
                            rule.getCategoria().getId(),
                            rule.getCategoria().getDescripcion(),
                            rule.getCategoria().getActivo()
                    );
                }
                return new CreateBudgetResponse.ReglaPresupuestoCategoriaResponse(
                        rule.getId(),
                        catResponse,
                        rule.getPorcentajeLimite(),
                        rule.getMontoLimiteCalculado(),
                        rule.isActivo()
                );
            }).collect(Collectors.toList());
        }

        return new CreateBudgetResponse(
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