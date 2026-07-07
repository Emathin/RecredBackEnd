package com.recred.backend.application;

import com.recred.backend.domain.exceptions.GradeNotFoundException;
import com.recred.backend.domain.repositories.GradoRepository;

public class GetGrade {

    private final GradoRepository repository;

    public GetGrade(GradoRepository repository) {
        this.repository = repository;
    }

    public GradeDetailsResponse execute(GetGradeRequest request) {
        GradeValidation.validateIds(request.colegioId(), request.gradeId());
        return repository.findByIdAndColegioId(request.gradeId(), request.colegioId())
                .map(GradeDetailsResponse::fromDomain)
                .orElseThrow(() -> new GradeNotFoundException(request.gradeId()));
    }
}
