package com.recred.backend.unit.application;

import com.recred.backend.application.AcceptCollaboratorInvitationUseCase;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.TutorCollaboratorInvitation;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.TutorCollaboratorInvitationRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.CognitoTutorIdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AcceptCollaboratorInvitationUseCaseTest {

    private TutorCollaboratorInvitationRepository invitationRepository;
    private TutorRepository tutorRepository;
    private UsuarioRepository usuarioRepository;
    private CognitoTutorIdentityService cognitoIdentityService;
    private AcceptCollaboratorInvitationUseCase useCase;

    @BeforeEach
    void setUp() {
        invitationRepository = mock(TutorCollaboratorInvitationRepository.class);
        tutorRepository = mock(TutorRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        cognitoIdentityService = mock(CognitoTutorIdentityService.class);
        useCase = new AcceptCollaboratorInvitationUseCase(
                invitationRepository, tutorRepository, usuarioRepository, cognitoIdentityService);
    }

    @Test
    void execute_successful() {
        String token = "valid_token";
        TutorCollaboratorInvitation invitation = new TutorCollaboratorInvitation();
        invitation.setToken(token);
        invitation.setTutorPrincipalId(UUID.randomUUID());
        invitation.setEmail("colab@test.com");
        invitation.setAssignedStudentIds(Set.of(UUID.randomUUID()));

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));
        when(cognitoIdentityService.createTutorUser(anyString(), anyString())).thenReturn("cognito-id-123");

        Tutor principal = new Tutor();
        principal.setId(invitation.getTutorPrincipalId());
        Alumno assigned = new Alumno();
        assigned.setId(invitation.getAssignedStudentIds().iterator().next());
        principal.setHijos(Set.of(assigned));
        when(tutorRepository.findById(invitation.getTutorPrincipalId())).thenReturn(Optional.of(principal));

        useCase.execute(token, "Nombre", "Apellido", "Pass1234!", "12345678", "1122334455");

        ArgumentCaptor<Usuario> userCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(userCaptor.capture());
        Usuario savedUser = userCaptor.getValue();
        assertEquals("Nombre", savedUser.getNombre());
        assertEquals("Apellido", savedUser.getApellido());
        assertEquals("colab@test.com", savedUser.getEmail());
        assertEquals(RolUsuario.PADRE, savedUser.getRol());

        ArgumentCaptor<Tutor> tutorCaptor = ArgumentCaptor.forClass(Tutor.class);
        verify(tutorRepository).save(tutorCaptor.capture());
        Tutor savedTutor = tutorCaptor.getValue();
        assertTrue(savedTutor.getEsColaborador());
        assertEquals(principal.getId(), savedTutor.getTutorPrincipalId());
        assertEquals(1, savedTutor.getHijos().size());

        verify(invitationRepository).delete(invitation);
    }

    @Test
    void execute_failsIfTokenNotFound() {
        when(invitationRepository.findByToken(anyString())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> 
                useCase.execute("invalid", "Nombre", "Apellido", "Pass1234!", "12345678", "1122334455"));
    }
}
