package com.recred.backend.unit.application;

import com.recred.backend.application.GetStudentRewardStatus;
import com.recred.backend.application.StudentRewardResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.RewardLevel;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStudentRewardStatusTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private GetStudentRewardStatus getStudentRewardStatus;

    @Test
    void execute_DebeRetornarEstadoBronce_CuandoTienePocosPuntos() {
        UUID alumnoId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        alumno.setPuntosRecompensa(50);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        StudentRewardResponse response = getStudentRewardStatus.execute(alumnoId);

        assertEquals(50, response.totalPoints());
        assertEquals("BRONZE", response.currentLevel());
        assertEquals(50, response.pointsToNextLevel());
        assertEquals("SILVER", response.nextLevelName());
    }

    @Test
    void execute_DebeRetornarEstadoOro_CuandoSuperaElUmbral() {
        UUID alumnoId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        alumno.setPuntosRecompensa(300);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        StudentRewardResponse response = getStudentRewardStatus.execute(alumnoId);

        assertEquals(300, response.totalPoints());
        assertEquals("GOLD", response.currentLevel());
        assertEquals(0, response.pointsToNextLevel());
        assertEquals("MAX", response.nextLevelName());
    }
}
