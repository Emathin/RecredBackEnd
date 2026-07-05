package com.recred.backend.application;

import com.recred.backend.domain.exceptions.GradeNotFoundException;
import com.recred.backend.domain.exceptions.LevelNotFoundException;
import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.Nivel;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.GradoRepository;
import com.recred.backend.domain.repositories.NivelRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class UpdateGrade {

    private final GradoRepository gradeRepository;
    private final ColegioRepository schoolRepository;
    private final NivelRepository levelRepository;

    public UpdateGrade(
            GradoRepository gradeRepository,
            ColegioRepository schoolRepository,
            NivelRepository levelRepository
    ) {
        this.gradeRepository = gradeRepository;
        this.schoolRepository = schoolRepository;
        this.levelRepository = levelRepository;
    }

    @Transactional
    public GradeDetailsResponse execute(UpdateGradeRequest request) {
        GradeValidation.validateIds(request.colegioId(), request.gradeId());
        GradeValidation.validateLevelId(request.nivelId());
        String anio = GradeValidation.validatedYear(request.anio());
        String division = GradeValidation.validatedDivision(request.division());
        requireActiveSchool(request);
        Nivel level = level(request.nivelId());
        Grado grade = grade(request);
        requireNoDuplicateIfActive(grade, request, anio, division);
        grade.setNivel(level);
        grade.setAnio(anio);
        grade.setDivision(division);
        return GradeDetailsResponse.fromDomain(gradeRepository.save(grade));
    }

    private void requireActiveSchool(UpdateGradeRequest request) {
        if (!schoolRepository.existsActiveById(request.colegioId())) {
            throw new SchoolNotFoundException(request.colegioId());
        }
    }

    private Nivel level(UUID nivelId) {
        return levelRepository.findActiveById(nivelId)
                .orElseThrow(() -> new LevelNotFoundException(nivelId));
    }

    private Grado grade(UpdateGradeRequest request) {
        return gradeRepository.findByIdAndColegioId(request.gradeId(), request.colegioId())
                .orElseThrow(() -> new GradeNotFoundException(request.gradeId()));
    }

    private void requireNoDuplicateIfActive(
            Grado grade,
            UpdateGradeRequest request,
            String anio,
            String division
    ) {
        if (Boolean.FALSE.equals(grade.getActivo())) {
            return;
        }

        GradeValidation.requireNoActiveDuplicate(
                gradeRepository,
                request.colegioId(),
                request.nivelId(),
                request.gradeId(),
                anio,
                division);
    }
}
