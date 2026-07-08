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
    public CreateBudgetResponse execute(CreateBudgetRequest createBudgetRequest) {
        PresupuestoAlumno budget = buildBudgetFrom(createBudgetRequest);
        applyBudgetRules(budget);
        PresupuestoAlumno savedBudget = repository.save(budget);

        return mapToResponse(savedBudget);
    }

    private PresupuestoAlumno buildBudgetFrom(CreateBudgetRequest createBudgetRequest) {
        List<ReglaPresupuestoCategoria> categoryRules =
                buildCategoryRules(createBudgetRequest.categoryRules());
        return buildBudget(createBudgetRequest, categoryRules);
    }

    private void applyBudgetRules(PresupuestoAlumno budget) {
        budget.validarPorcentajes();
        budget.recalcularMontosReglas();
    }

    private List<ReglaPresupuestoCategoria> buildCategoryRules(
            List<CreateBudgetRequest.CategoryRuleRequest> categoryRules) {
        if (categoryRules == null || categoryRules.isEmpty()) {
            return null;
        }
        return categoryRules.stream()
                .map(this::buildCategoryRule)
                .collect(Collectors.toList());
    }

    private ReglaPresupuestoCategoria buildCategoryRule(
            CreateBudgetRequest.CategoryRuleRequest categoryRuleRequest) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(categoryRuleRequest.categoryId());

        return new ReglaPresupuestoCategoria(
                null,
                category,
                categoryRuleRequest.porcentajeLimite(),
                null,
                true);
    }

    private PresupuestoAlumno buildBudget(
            CreateBudgetRequest createBudgetRequest,
            List<ReglaPresupuestoCategoria> categoryRules) {
        return new PresupuestoAlumno(
                null,
                createBudgetRequest.studentId(),
                createBudgetRequest.creadorId(),
                createBudgetRequest.limitAmount(),
                createBudgetRequest.period(),
                createBudgetRequest.startDate(),
                true,
                categoryRules);
    }

    private CreateBudgetResponse mapToResponse(PresupuestoAlumno budget) {
        return new CreateBudgetResponse(
                budget.getId(),
                budget.getAlumnoId(),
                budget.getCreadoPorUsuarioId(),
                budget.getMontoLimiteGeneral(),
                budget.getPeriodo(),
                budget.getFechaInicio(),
                budget.isActivo(),
                mapRuleResponses(budget),
                budget.getMontoLimite()
        );
    }

    private List<CreateBudgetResponse.ReglaPresupuestoCategoriaResponse> mapRuleResponses(
            PresupuestoAlumno budget) {
        if (budget.getReglasCategoria() == null) {
            return null;
        }

        return budget.getReglasCategoria().stream()
                .map(this::mapRuleResponse)
                .collect(Collectors.toList());
    }

    private CreateBudgetResponse.ReglaPresupuestoCategoriaResponse mapRuleResponse(
            ReglaPresupuestoCategoria rule) {
        return new CreateBudgetResponse.ReglaPresupuestoCategoriaResponse(
                rule.getId(),
                mapCategoryResponse(rule.getCategoria()),
                rule.getPorcentajeLimite(),
                rule.getMontoLimiteCalculado(),
                rule.isActivo()
        );
    }

    private CreateBudgetResponse.CategoriaProductoResponse mapCategoryResponse(
            CategoriaProducto category) {
        if (category == null) {
            return null;
        }

        return new CreateBudgetResponse.CategoriaProductoResponse(
                category.getId(),
                category.getDescripcion(),
                category.getActivo()
        );
    }
}
