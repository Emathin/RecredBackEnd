package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.RewardLevel;
import com.recred.backend.domain.repositories.AlumnoRepository;

import java.util.UUID;

public class GetStudentRewardStatus {

    private final AlumnoRepository alumnoRepository;

    public GetStudentRewardStatus(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    public StudentRewardResponse execute(UUID studentId) {
        Alumno student = alumnoRepository.findById(studentId)
                .orElseThrow(() -> new AlumnoNoEncontradoException(studentId));

        RewardLevel currentLevel = student.calculateCurrentLevel();
        Integer nextPoints = student.getPointsToNextLevel();
        String nextName = determineNextLevelName(currentLevel);

        return new StudentRewardResponse(
                student.getPuntosRecompensa(),
                currentLevel.name(),
                currentLevel.getMessage(),
                nextPoints,
                nextName
        );
    }

    private String determineNextLevelName(RewardLevel current) {
        return switch (current) {
            case BRONZE -> RewardLevel.SILVER.name();
            case SILVER -> RewardLevel.GOLD.name();
            case GOLD -> "MAX";
        };
    }
}
