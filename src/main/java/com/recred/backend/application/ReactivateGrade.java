package com.recred.backend.application;

import com.recred.backend.domain.exceptions.GradeNotFoundException;
import com.recred.backend.domain.exceptions.InvalidGradeException;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.repositories.GradoRepository;
import org.springframework.transaction.annotation.Transactional;

public class ReactivateGrade {

    private final GradoRepository repository;

    public ReactivateGrade(GradoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public GradeDetailsResponse execute(ReactivateGradeRequest request) {
        GradeValidation.validateIds(request.colegioId(), request.gradeId());
        Grado grade = grade(request);
        validateLevel(grade);
        GradeValidation.requireNoActiveDuplicate(
                repository,
                request.colegioId(),
                grade.getNivel().getId(),
                request.gradeId(),
                grade.getAnio(),
                grade.getDivision());
        grade.setActivo(true);
        return GradeDetailsResponse.fromDomain(repository.save(grade));
    }

    private Grado grade(ReactivateGradeRequest request) {
        return repository.findByIdAndColegioId(request.gradeId(), request.colegioId())
                .orElseThrow(() -> new GradeNotFoundException(request.gradeId()));
    }

    private void validateLevel(Grado grade) {
        if (grade.getNivel() == null || grade.getNivel().getId() == null) {
            throw new InvalidGradeException("El nivel del grado es requerido");
        }
    }
}
