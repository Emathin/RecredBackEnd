package com.recred.backend.unit.application;

import com.recred.backend.application.ListCollaboratorsUseCase;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.TutorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListCollaboratorsUseCaseTest {

    private TutorRepository tutorRepository;
    private ListCollaboratorsUseCase useCase;

    @BeforeEach
    void setUp() {
        tutorRepository = mock(TutorRepository.class);
        useCase = new ListCollaboratorsUseCase(tutorRepository);
    }

    @Test
    void execute_successful() {
        UUID tutorPrincipalId = UUID.randomUUID();
        Tutor principal = new Tutor();
        principal.setId(tutorPrincipalId);
        principal.setEsColaborador(false);

        Tutor colab1 = new Tutor();
        colab1.setId(UUID.randomUUID());
        colab1.setEsColaborador(true);
        colab1.setTutorPrincipalId(tutorPrincipalId);

        when(tutorRepository.findById(tutorPrincipalId)).thenReturn(Optional.of(principal));
        when(tutorRepository.findByTutorPrincipalId(tutorPrincipalId)).thenReturn(List.of(colab1));

        List<Tutor> result = useCase.execute(tutorPrincipalId);
        assertEquals(1, result.size());
        assertEquals(colab1.getId(), result.get(0).getId());
    }

    @Test
    void execute_failsIfTutorIsCollaborator() {
        UUID tutorPrincipalId = UUID.randomUUID();
        Tutor principal = new Tutor();
        principal.setId(tutorPrincipalId);
        principal.setEsColaborador(true);

        when(tutorRepository.findById(tutorPrincipalId)).thenReturn(Optional.of(principal));

        assertThrows(SecurityException.class, () -> useCase.execute(tutorPrincipalId));
    }
}
