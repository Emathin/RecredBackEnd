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

    public StudentRewardResponse execute(GetStudentRewardStatusRequest rewardRequest) {
        Alumno student = findStudent(rewardRequest);
        RewardLevel currentLevel = student.calculateCurrentLevel();

        return rewardResponseFrom(student, currentLevel);
    }

    private Alumno findStudent(GetStudentRewardStatusRequest rewardRequest) {
        return alumnoRepository.findById(rewardRequest.alumnoId())
                .orElseThrow(() -> new AlumnoNoEncontradoException(rewardRequest.alumnoId()));
    }

    private StudentRewardResponse rewardResponseFrom(Alumno student, RewardLevel currentLevel) {
        return new StudentRewardResponse(
                student.getPuntosRecompensa(),
                currentLevel.name(),
                currentLevel.getMessage(),
                student.getPointsToNextLevel(),
                nextLevelNameAfter(currentLevel)
        );
    }

    private String nextLevelNameAfter(RewardLevel currentLevel) {
        return switch (currentLevel) {
            case BRONZE -> RewardLevel.SILVER.name();
            case SILVER -> RewardLevel.GOLD.name();
            case GOLD -> "MAX";
        };
    }
}
