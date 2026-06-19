package com.recred.backend.unit.application;

import com.recred.backend.application.GetStudentRewardStatus;
import com.recred.backend.application.StudentRewardResponse;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStudentRewardStatusTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private GetStudentRewardStatus getStudentRewardStatus;

    @Test
    void executeWhenStudentHasFewPointsShouldReturnBronzeStatus() {
        UUID studentId = UUID.randomUUID();
        Alumno student = new Alumno();
        student.setId(studentId);
        student.setPuntosRecompensa(50);

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));

        StudentRewardResponse response = getStudentRewardStatus.execute(studentId);

        assertEquals(50, response.totalPoints());
        assertEquals("BRONZE", response.currentLevel());
        assertEquals(50, response.pointsToNextLevel());
        assertEquals("SILVER", response.nextLevelName());
    }

    @Test
    void executeWhenStudentPassesGoldThresholdShouldReturnGoldStatus() {
        UUID studentId = UUID.randomUUID();
        Alumno student = new Alumno();
        student.setId(studentId);
        student.setPuntosRecompensa(300);

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));

        StudentRewardResponse response = getStudentRewardStatus.execute(studentId);

        assertEquals(300, response.totalPoints());
        assertEquals("GOLD", response.currentLevel());
        assertEquals(0, response.pointsToNextLevel());
        assertEquals("MAX", response.nextLevelName());
    }

    @Test
    void executeWhenStudentPassesFirstThresholdShouldReturnSilverStatus() {
        UUID studentId = UUID.randomUUID();
        Alumno student = new Alumno();
        student.setId(studentId);
        student.setPuntosRecompensa(150);

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));

        StudentRewardResponse response = getStudentRewardStatus.execute(studentId);

        assertEquals("SILVER", response.currentLevel());
        assertEquals(100, response.pointsToNextLevel());
        assertEquals("GOLD", response.nextLevelName());
    }

    @Test
    void executeWhenStudentDoesNotExistShouldThrowException() {
        UUID studentId = UUID.randomUUID();

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThrows(AlumnoNoEncontradoException.class,
                () -> getStudentRewardStatus.execute(studentId));
    }
}
