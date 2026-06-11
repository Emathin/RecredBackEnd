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

    public StudentRewardResponse execute(UUID alumnoId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new AlumnoNoEncontradoException(alumnoId));

        RewardLevel currentLevel = alumno.calculateCurrentLevel();
        Integer nextPoints = alumno.getPointsToNextLevel();
        String nextName = determineNextLevelName(currentLevel);

        return new StudentRewardResponse(
                alumno.getPuntosRecompensa(),
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
