package com.recred.backend.application;

import com.recred.backend.domain.exceptions.GradeAlreadyExistsException;
import com.recred.backend.domain.exceptions.InvalidGradeException;
import com.recred.backend.domain.repositories.GradoRepository;

import java.util.UUID;

final class GradeValidation {

    private GradeValidation() {
    }

    static void validateIds(UUID colegioId, UUID gradeId) {
        validateSchoolId(colegioId);
        if (gradeId == null) {
            throw new InvalidGradeException("gradeId es requerido");
        }
    }

    static void validateSchoolId(UUID colegioId) {
        if (colegioId == null) {
            throw new InvalidGradeException("colegioId es requerido");
        }
    }

    static void validateLevelId(UUID nivelId) {
        if (nivelId == null) {
            throw new InvalidGradeException("nivelId es requerido");
        }
    }

    static String validatedYear(String anio) {
        if (anio == null || anio.isBlank()) {
            throw new InvalidGradeException("El año del grado es requerido");
        }

        return anio.trim();
    }

    static String validatedDivision(String division) {
        if (division == null || division.isBlank()) {
            throw new InvalidGradeException("La division del grado es requerida");
        }

        return division.trim();
    }

    static void requireNoActiveDuplicate(
            GradoRepository repository,
            UUID colegioId,
            UUID nivelId,
            UUID excludedId,
            String anio,
            String division
    ) {
        boolean exists = repository.existsActiveByColegioIdAndNivelIdAndAnioAndDivision(
                colegioId,
                nivelId,
                excludedId,
                anio,
                division);
        if (exists) {
            throw new GradeAlreadyExistsException(anio, division);
        }
    }
}
