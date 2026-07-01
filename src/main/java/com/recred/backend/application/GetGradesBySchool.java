package com.recred.backend.application;

import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.GradoRepository;

import java.util.List;
import java.util.UUID;

public class GetGradesBySchool {

    private final ColegioRepository schoolRepository;
    private final GradoRepository gradeRepository;

    public GetGradesBySchool(ColegioRepository schoolRepository,
                             GradoRepository gradeRepository) {
        this.schoolRepository = schoolRepository;
        this.gradeRepository = gradeRepository;
    }

    public GetGradesBySchoolResponse execute(GetGradesBySchoolRequest request) {
        request.validate();

        UUID schoolId = request.schoolId();
        ensureSchoolExists(schoolId);
        List<Grado> grades = findActiveGrades(schoolId);

        return toResponse(grades);
    }

    private void ensureSchoolExists(UUID schoolId) {
        if (!schoolRepository.existsActiveById(schoolId)) {
            throw new SchoolNotFoundException(schoolId);
        }
    }

    private List<Grado> findActiveGrades(UUID schoolId) {
        return gradeRepository.findAllActiveByColegioId(schoolId);
    }

    private GetGradesBySchoolResponse toResponse(List<Grado> grades) {
        return GetGradesBySchoolResponse.fromDomain(grades);
    }
}
