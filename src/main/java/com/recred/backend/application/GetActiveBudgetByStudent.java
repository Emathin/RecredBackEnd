package com.recred.backend.application;

import com.recred.backend.domain.models.PresupuestoAlumno;
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
        List<GetActiveBudgetByStudentResponse.ReglaPresupuestoCategoriaResponse> rules = null;
        if (domain.getReglasCategoria() != null) {
            rules = domain.getReglasCategoria().stream().map(rule -> {
                GetActiveBudgetByStudentResponse.CategoriaProductoResponse catResponse = null;
                if (rule.getCategoria() != null) {
                    catResponse = new GetActiveBudgetByStudentResponse.CategoriaProductoResponse(
                            rule.getCategoria().getId(),
                            rule.getCategoria().getDescripcion(),
                            rule.getCategoria().getActivo()
                    );
                }
                return new GetActiveBudgetByStudentResponse.ReglaPresupuestoCategoriaResponse(
                        rule.getId(),
                        catResponse,
                        rule.getPorcentajeLimite(),
                        rule.getMontoLimiteCalculado(),
                        rule.isActivo()
                );
            }).collect(Collectors.toList());
        }

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
}