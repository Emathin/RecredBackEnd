package com.recred.backend.unit.application;

import com.recred.backend.application.GetBuffetByStudent;
import com.recred.backend.application.GetBuffetByStudentRequest;
import com.recred.backend.application.GetBuffetByStudentResponse;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.BuffetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetBuffetByStudentTest {

    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private BuffetRepository buffetRepository;

    private GetBuffetByStudent useCase;

    private UUID studentId;
    private Buffet buffet;
    private Optional<GetBuffetByStudentResponse> response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new GetBuffetByStudent(buffetRepository, alumnoRepository);
    }

    @Test
    void givenExistingStudentWithBuffet_whenGettingBuffet_thenReturnsBuffet() {
        givenStudentScenario();
        givenStudentExists();
        givenStudentHasBuffet("Buffet Central");

        whenGettingBuffetByStudent();

        thenBuffetIsReturned();
        thenBuffetWasRequestedForStudent();
    }

    @Test
    void givenMissingStudent_whenGettingBuffet_thenFailsWithoutSearchingBuffet() {
        givenStudentScenario();
        givenStudentDoesNotExist();

        whenGettingBuffetByStudentFails();

        thenStudentNotFoundIsThrown();
        thenBuffetWasNotRequested();
    }

    @Test
    void givenExistingStudentWithoutBuffet_whenGettingBuffet_thenReturnsEmpty() {
        givenStudentScenario();
        givenStudentExists();
        givenStudentHasNoBuffet();

        whenGettingBuffetByStudent();

        thenBuffetIsEmpty();
        thenBuffetWasRequestedForStudent();
    }

    private void givenStudentScenario() {
        studentId = UUID.randomUUID();
    }

    private void givenStudentExists() {
        when(alumnoRepository.existsById(studentId)).thenReturn(true);
    }

    private void givenStudentDoesNotExist() {
        when(alumnoRepository.existsById(studentId)).thenReturn(false);
    }

    private void givenStudentHasBuffet(String name) {
        buffet = new Buffet();
        buffet.setId(UUID.randomUUID());
        buffet.setNombre(name);
        when(buffetRepository.findByAlumnoId(studentId)).thenReturn(Optional.of(buffet));
    }

    private void givenStudentHasNoBuffet() {
        when(buffetRepository.findByAlumnoId(studentId)).thenReturn(Optional.empty());
    }

    private void whenGettingBuffetByStudent() {
        response = useCase.execute(new GetBuffetByStudentRequest(studentId));
    }

    private void whenGettingBuffetByStudentFails() {
        thrownException = assertThrows(RuntimeException.class, () ->
                useCase.execute(new GetBuffetByStudentRequest(studentId)));
    }

    private void thenBuffetIsReturned() {
        assertTrue(response.isPresent());
        assertEquals(buffet.getId(), response.get().id());
        assertEquals("Buffet Central", response.get().nombre());
    }

    private void thenStudentNotFoundIsThrown() {
        assertTrue(thrownException instanceof AlumnoNoEncontradoException);
    }

    private void thenBuffetIsEmpty() {
        assertFalse(response.isPresent());
    }

    private void thenBuffetWasRequestedForStudent() {
        verify(buffetRepository).findByAlumnoId(studentId);
    }

    private void thenBuffetWasNotRequested() {
        verifyNoInteractions(buffetRepository);
    }
}
