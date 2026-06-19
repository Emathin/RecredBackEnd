package com.recred.backend.unit.application;

import com.recred.backend.application.GetHealthRestrictions;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetHealthRestrictionsTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private GetHealthRestrictions useCase;

    private UUID studentId;
    private Alumno student;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        student = new Alumno();
        student.setId(studentId);
    }

    @Test
    void executeWhenStudentExistsShouldReturnRestrictions() {
        ClasificacionSalud classification = new ClasificacionSalud();
        classification.setId(UUID.randomUUID());
        student.getRestriccionesSalud().add(classification);

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));

        Set<ClasificacionSalud> result = useCase.execute(studentId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(classification));
    }

    @Test
    void executeWhenStudentDoesNotExistShouldThrowException() {
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            useCase.execute(studentId);
        });

        assertEquals("El alumno con ID " + studentId + " no existe.", exception.getMessage());
    }
}
