package com.recred.backend.application;

import com.recred.backend.domain.exceptions.GradeNotFoundException;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.repositories.GradoRepository;
import org.springframework.transaction.annotation.Transactional;

public class DeactivateGrade {

    private final GradoRepository repository;

    public DeactivateGrade(GradoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public GradeDetailsResponse execute(DeactivateGradeRequest request) {
        GradeValidation.validateIds(request.colegioId(), request.gradeId());
        Grado grade = repository.findByIdAndColegioId(request.gradeId(), request.colegioId())
                .orElseThrow(() -> new GradeNotFoundException(request.gradeId()));
        grade.setActivo(false);
        return GradeDetailsResponse.fromDomain(repository.save(grade));
    }
}
