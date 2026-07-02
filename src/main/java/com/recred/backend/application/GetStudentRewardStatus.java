package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.RewardLevel;
import com.recred.backend.domain.repositories.AlumnoRepository;

public class GetStudentRewardStatus {

    private final AlumnoRepository alumnoRepository;

    public GetStudentRewardStatus(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    public StudentRewardResponse execute(GetStudentRewardStatusRequest request) {
        Alumno alumno = alumnoRepository.findById(request.alumnoId())
                .orElseThrow(() -> new AlumnoNoEncontradoException(request.alumnoId()));

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
