package com.recred.backend.unit.application;

import com.recred.backend.application.GetStudentRewardStatus;
import com.recred.backend.application.GetStudentRewardStatusRequest;
import com.recred.backend.application.StudentRewardResponse;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.RewardLevel;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStudentRewardStatusTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    private GetStudentRewardStatus useCase;

    private UUID studentId;
    private Alumno student;
    private StudentRewardResponse response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new GetStudentRewardStatus(alumnoRepository);
    }

    @Test
    void givenStudentWithPrincipiantePoints_whenGettingRewardStatus_thenReturnsPrincipianteStatus() {
        givenRewardStatusScenario();
        givenStudentHasRewardPoints(50);
        givenStudentExists();

        whenGettingRewardStatus();

        thenRewardStatusIs(50, RewardLevel.PRINCIPIANTE, 50, RewardLevel.CRACK.name());
    }

    @Test
    void givenStudentWithCrackPoints_whenGettingRewardStatus_thenReturnsCrackStatus() {
        givenRewardStatusScenario();
        givenStudentHasRewardPoints(150);
        givenStudentExists();

        whenGettingRewardStatus();

        thenRewardStatusIs(150, RewardLevel.CRACK, 100, RewardLevel.GOAT.name());
    }

    @Test
    void givenStudentWithGoatPoints_whenGettingRewardStatus_thenReturnsGoatStatus() {
        givenRewardStatusScenario();
        givenStudentHasRewardPoints(300);
        givenStudentExists();

        whenGettingRewardStatus();

        thenRewardStatusIs(300, RewardLevel.GOAT, 0, "MAX");
    }

    @Test
    void givenMissingStudent_whenGettingRewardStatus_thenFails() {
        givenRewardStatusScenario();
        givenStudentDoesNotExist();

        whenGettingRewardStatusFails();

        thenStudentNotFoundIsThrown();
    }

    private void givenRewardStatusScenario() {
        studentId = UUID.randomUUID();
        student = new Alumno();
        student.setId(studentId);
    }

    private void givenStudentHasRewardPoints(int points) {
        student.setPuntosRecompensa(points);
    }

    private void givenStudentExists() {
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
    }

    private void givenStudentDoesNotExist() {
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());
    }

    private void whenGettingRewardStatus() {
        response = useCase.execute(new GetStudentRewardStatusRequest(studentId));
    }

    private void whenGettingRewardStatusFails() {
        thrownException = assertThrows(RuntimeException.class, () ->
                useCase.execute(new GetStudentRewardStatusRequest(studentId)));
    }

    private void thenRewardStatusIs(
            int totalPoints,
            RewardLevel currentLevel,
            int pointsToNextLevel,
            String nextLevelName
    ) {
        assertEquals(totalPoints, response.totalPoints());
        assertEquals(currentLevel.name(), response.currentLevel());
        assertEquals(currentLevel.getMessage(), response.levelMessage());
        assertEquals(pointsToNextLevel, response.pointsToNextLevel());
        assertEquals(nextLevelName, response.nextLevelName());
    }

    private void thenStudentNotFoundIsThrown() {
        assertTrue(thrownException instanceof AlumnoNoEncontradoException);
    }
}
