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
        List<ReglaPresupuestoCategoria> reglas = null;

        if (request.categoryRules() != null && !request.categoryRules().isEmpty()) {
            reglas = request.categoryRules().stream().map(rule -> {
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
                request.studentId(),
                request.creadorId(),
                request.limitAmount(),
                request.period(),
                request.startDate(),
                true,
                reglas);

        presupuesto.validarPorcentajes();
        presupuesto.recalcularMontosReglas();

        PresupuestoAlumno saved = repository.save(presupuesto);
        return mapToResponse(saved);
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