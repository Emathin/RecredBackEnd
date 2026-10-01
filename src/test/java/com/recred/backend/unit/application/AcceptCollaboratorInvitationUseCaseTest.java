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
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.services.CognitoTutorIdentityService;
import com.recred.backend.domain.services.CreatedCognitoUser;
import com.recred.backend.application.TutorInvitationTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private AlumnoRepository alumnoRepository;
    private CognitoTutorIdentityService cognitoIdentityService;
    private TutorInvitationTokenService tokenService;
    private AcceptCollaboratorInvitationUseCase useCase;

    @BeforeEach
    void setUp() {
        invitationRepository = mock(TutorCollaboratorInvitationRepository.class);
        tutorRepository = mock(TutorRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        alumnoRepository = mock(AlumnoRepository.class);
        cognitoIdentityService = mock(CognitoTutorIdentityService.class);
        tokenService = mock(TutorInvitationTokenService.class);
        useCase = new AcceptCollaboratorInvitationUseCase(
                invitationRepository, tutorRepository, usuarioRepository, alumnoRepository, cognitoIdentityService, tokenService);
    }

    @Test
    void execute_successful() {
        String rawToken = "raw_token";
        String tokenHash = "token_hash";
        TutorCollaboratorInvitation invitation = new TutorCollaboratorInvitation();
        invitation.setTokenHash(tokenHash);
        invitation.setTutorPrincipalId(UUID.randomUUID());
        invitation.setEmail("colab@test.com");
        invitation.setAssignedStudentIds(Set.of(UUID.randomUUID()));
        invitation.setStatus(com.recred.backend.domain.models.TutorInvitationStatus.PENDING);
        invitation.setExpiraEn(java.time.LocalDateTime.now().plusDays(1));

        when(tokenService.hash(rawToken)).thenReturn(tokenHash);
        when(invitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(invitation));
        CreatedCognitoUser mockUser = mock(CreatedCognitoUser.class);
        when(mockUser.sub()).thenReturn("cognito-id-123");
        when(cognitoIdentityService.createTutorUser(org.mockito.ArgumentMatchers.isNull(), anyString(), anyString(), anyString())).thenReturn(mockUser);

        Tutor principal = new Tutor();
        principal.setId(invitation.getTutorPrincipalId());
        Usuario u = new Usuario();
        u.setPlan(com.recred.backend.domain.models.TipoPlan.AVANZADO);
        principal.setUsuario(u);
        Alumno assigned = new Alumno();
        assigned.setId(invitation.getAssignedStudentIds().iterator().next());
        principal.setHijos(Set.of(assigned));
        when(tutorRepository.findById(invitation.getTutorPrincipalId())).thenReturn(Optional.of(principal));
        when(alumnoRepository.findById(any(UUID.class))).thenReturn(Optional.of(assigned));

        useCase.execute(rawToken, "Nombre", "Apellido", "Pass1234!", "12345678", "1122334455");

        ArgumentCaptor<Usuario> userCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(userCaptor.capture());
        Usuario savedUser = userCaptor.getValue();
        assertEquals("Nombre", savedUser.getNombre());
        assertEquals("Apellido", savedUser.getApellido());
        assertEquals("colab@test.com", savedUser.getEmail());
        assertEquals(RolUsuario.PADRE, savedUser.getRol());

        Tutor savedTutor = savedUser.getTutor();
        assertNotNull(savedTutor);
        assertTrue(savedTutor.getEsColaborador());
        assertEquals(principal.getId(), savedTutor.getTutorPrincipalId());
        assertEquals(1, savedTutor.getHijos().size());

        verify(invitationRepository).save(invitation);
    }

    @Test
    void execute_failsIfTokenNotFound() {
        when(tokenService.hash(anyString())).thenReturn("hash");
        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> 
                useCase.execute("invalid", "Nombre", "Apellido", "Pass1234!", "12345678", "1122334455"));
    }
}
