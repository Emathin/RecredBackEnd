package com.recred.backend.application;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.ReglaPresupuestoCategoria;
import com.recred.backend.domain.repositories.PresupuestoRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GetActiveBudgetByStudent {

    private final PresupuestoRepository repository;

    public GetActiveBudgetByStudent(PresupuestoRepository repository) {
        this.repository = repository;
    }

    public Optional<GetActiveBudgetByStudentResponse> execute(
            GetActiveBudgetByStudentRequest request) {
        return repository.findActivoByAlumnoId(request.studentId())
                .map(this::mapToResponse);
    }

    private GetActiveBudgetByStudentResponse mapToResponse(PresupuestoAlumno domain) {
        List<GetActiveBudgetByStudentResponse.ReglaPresupuestoCategoriaResponse> rules =
                this.mapRulesResponse(domain.getReglasCategoria());

        return new GetActiveBudgetByStudentResponse(
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

    private List<GetActiveBudgetByStudentResponse.ReglaPresupuestoCategoriaResponse>
            mapRulesResponse(List<ReglaPresupuestoCategoria> rules) {
        if (rules == null) {
            return null;
        }
        return rules.stream().map(this::mapRuleResponse).collect(Collectors.toList());
    }

    private GetActiveBudgetByStudentResponse.ReglaPresupuestoCategoriaResponse
            mapRuleResponse(ReglaPresupuestoCategoria rule) {
        GetActiveBudgetByStudentResponse.CategoriaProductoResponse catResponse =
                this.mapCategoryResponse(rule.getCategoria());

        return new GetActiveBudgetByStudentResponse.ReglaPresupuestoCategoriaResponse(
                rule.getId(),
                catResponse,
                rule.getPorcentajeLimite(),
                rule.getMontoLimiteCalculado(),
                rule.isActivo()
        );
    }

    private GetActiveBudgetByStudentResponse.CategoriaProductoResponse mapCategoryResponse(
            CategoriaProducto categoria) {
        if (categoria == null) {
            return null;
        }
        return new GetActiveBudgetByStudentResponse.CategoriaProductoResponse(
                categoria.getId(),
                categoria.getDescripcion(),
                categoria.getActivo()
        );
    }
}