package com.recred.backend.unit.application;

import com.recred.backend.application.GetBuffetByStudent;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.BuffetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetBuffetByStudentTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private BuffetRepository buffetRepository;

    @InjectMocks
    private GetBuffetByStudent getBuffetByStudent;

    @Test
    void executeWhenStudentExistsAndBuffetIsActiveShouldReturnBuffet() {
        UUID studentId = UUID.randomUUID();
        Buffet buffet = new Buffet();
        buffet.setId(UUID.randomUUID());
        buffet.setNombre("Buffet Central");

        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(buffetRepository.findByAlumnoId(studentId)).thenReturn(Optional.of(buffet));

        Optional<Buffet> result = getBuffetByStudent.execute(studentId);

        assertTrue(result.isPresent());
        assertEquals("Buffet Central", result.get().getNombre());
        verify(alumnoRepository, times(1)).existsById(studentId);
        verify(buffetRepository, times(1)).findByAlumnoId(studentId);
    }

    @Test
    void executeWhenStudentDoesNotExistShouldThrowStudentNotFoundException() {
        UUID studentId = UUID.randomUUID();
        when(alumnoRepository.existsById(studentId)).thenReturn(false);

        assertThrows(AlumnoNoEncontradoException.class, () ->
                getBuffetByStudent.execute(studentId)
        );

        verify(alumnoRepository, times(1)).existsById(studentId);
        verifyNoInteractions(buffetRepository);
    }

    @Test
    void executeWhenStudentExistsButHasNoBuffetShouldReturnEmpty() {
        UUID studentId = UUID.randomUUID();
        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(buffetRepository.findByAlumnoId(studentId)).thenReturn(Optional.empty());

        Optional<Buffet> result = getBuffetByStudent.execute(studentId);

        assertFalse(result.isPresent());
        verify(alumnoRepository, times(1)).existsById(studentId);
        verify(buffetRepository, times(1)).findByAlumnoId(studentId);
    }
}
