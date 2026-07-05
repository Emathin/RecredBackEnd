package com.recred.backend.application;

import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.GradoRepository;

import java.util.List;

public class ListSchoolGrades {

    private final GradoRepository gradeRepository;
    private final ColegioRepository schoolRepository;

    public ListSchoolGrades(
            GradoRepository gradeRepository,
            ColegioRepository schoolRepository
    ) {
        this.gradeRepository = gradeRepository;
        this.schoolRepository = schoolRepository;
    }

    public List<GradeDetailsResponse> execute(ListSchoolGradesRequest request) {
        GradeValidation.validateSchoolId(request.colegioId());
        requireActiveSchool(request);
        return grades(request).stream()
                .map(GradeDetailsResponse::fromDomain)
                .toList();
    }

    private void requireActiveSchool(ListSchoolGradesRequest request) {
        if (!schoolRepository.existsActiveById(request.colegioId())) {
            throw new SchoolNotFoundException(request.colegioId());
        }
    }

    private List<Grado> grades(ListSchoolGradesRequest request) {
        if (request.includeInactive()) {
            return gradeRepository.findAllByColegioIdIncludingInactive(request.colegioId());
        }

        return gradeRepository.findAllActiveByColegioId(request.colegioId());
    }
}
