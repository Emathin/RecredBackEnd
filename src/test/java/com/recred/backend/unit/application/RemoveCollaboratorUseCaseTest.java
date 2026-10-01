package com.recred.backend.unit.application;

import com.recred.backend.application.RemoveCollaboratorUseCase;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.services.CognitoTutorIdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemoveCollaboratorUseCaseTest {

    private TutorRepository tutorRepository;
    private RemoveCollaboratorUseCase useCase;

    @BeforeEach
    void setUp() {
        tutorRepository = mock(TutorRepository.class);
        useCase = new RemoveCollaboratorUseCase(tutorRepository);
    }

    @Test
    void execute_successful() {
        UUID tutorPrincipalId = UUID.randomUUID();
        UUID collaboratorId = UUID.randomUUID();

        Tutor principal = new Tutor();
        principal.setId(tutorPrincipalId);
        principal.setEsColaborador(false);

        Tutor colab = new Tutor();
        colab.setId(collaboratorId);
        colab.setEsColaborador(true);
        colab.setTutorPrincipalId(tutorPrincipalId);
        Usuario usuario = new Usuario();
        usuario.setIdCognito("cognito-id-123");
        colab.setUsuario(usuario);

        when(tutorRepository.findById(tutorPrincipalId)).thenReturn(Optional.of(principal));
        when(tutorRepository.findById(collaboratorId)).thenReturn(Optional.of(colab));

        useCase.execute(tutorPrincipalId, collaboratorId);

        verify(tutorRepository).save(colab);
    }

    @Test
    void execute_failsIfTutorIsCollaborator() {
        UUID tutorPrincipalId = UUID.randomUUID();
        UUID collaboratorId = UUID.randomUUID();

        Tutor principal = new Tutor();
        principal.setId(tutorPrincipalId);
        principal.setEsColaborador(true);

        when(tutorRepository.findById(tutorPrincipalId)).thenReturn(Optional.of(principal));

        assertThrows(SecurityException.class, () -> useCase.execute(tutorPrincipalId, collaboratorId));
    }

    @Test
    void execute_failsIfTargetIsNotCollaboratorOfPrincipal() {
        UUID tutorPrincipalId = UUID.randomUUID();
        UUID collaboratorId = UUID.randomUUID();

        Tutor principal = new Tutor();
        principal.setId(tutorPrincipalId);
        principal.setEsColaborador(false);

        Tutor colab = new Tutor();
        colab.setId(collaboratorId);
        colab.setEsColaborador(true);
        colab.setTutorPrincipalId(UUID.randomUUID()); // Different principal

        when(tutorRepository.findById(tutorPrincipalId)).thenReturn(Optional.of(principal));
        when(tutorRepository.findById(collaboratorId)).thenReturn(Optional.of(colab));

        assertThrows(SecurityException.class, () -> useCase.execute(tutorPrincipalId, collaboratorId));
    }
}
