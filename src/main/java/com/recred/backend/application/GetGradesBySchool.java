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

    public List<Grado> execute(UUID schoolId) {
        if (!schoolRepository.existsActiveById(schoolId)) {
            throw new SchoolNotFoundException(schoolId);
        }

        return gradeRepository.findAllActiveByColegioId(schoolId);
    }
}
