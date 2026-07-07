package com.recred.backend.application;

import com.recred.backend.domain.exceptions.LevelNotFoundException;
import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.Nivel;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.GradoRepository;
import com.recred.backend.domain.repositories.NivelRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class CreateGrade {

    private final GradoRepository gradeRepository;
    private final ColegioRepository schoolRepository;
    private final NivelRepository levelRepository;

    public CreateGrade(
            GradoRepository gradeRepository,
            ColegioRepository schoolRepository,
            NivelRepository levelRepository
    ) {
        this.gradeRepository = gradeRepository;
        this.schoolRepository = schoolRepository;
        this.levelRepository = levelRepository;
    }

    @Transactional
    public GradeDetailsResponse execute(CreateGradeRequest request) {
        GradeValidation.validateSchoolId(request.colegioId());
        GradeValidation.validateLevelId(request.nivelId());
        String anio = GradeValidation.validatedYear(request.anio());
        String division = GradeValidation.validatedDivision(request.division());
        requireActiveSchool(request.colegioId());
        Nivel level = level(request.nivelId());
        GradeValidation.requireNoActiveDuplicate(
                gradeRepository,
                request.colegioId(),
                request.nivelId(),
                null,
                anio,
                division);

        return GradeDetailsResponse.fromDomain(gradeRepository.save(
                grade(request.colegioId(), level, anio, division)));
    }

    private void requireActiveSchool(UUID colegioId) {
        if (!schoolRepository.existsActiveById(colegioId)) {
            throw new SchoolNotFoundException(colegioId);
        }
    }

    private Nivel level(UUID nivelId) {
        return levelRepository.findActiveById(nivelId)
                .orElseThrow(() -> new LevelNotFoundException(nivelId));
    }

    private Grado grade(UUID colegioId, Nivel level, String anio, String division) {
        Grado grade = new Grado();
        grade.setColegio(schoolRef(colegioId));
        grade.setNivel(level);
        grade.setAnio(anio);
        grade.setDivision(division);
        grade.setActivo(true);
        return grade;
    }

    private Colegio schoolRef(UUID colegioId) {
        Colegio school = new Colegio();
        school.setId(colegioId);
        return school;
    }
}
