package com.recred.backend.unit.application;

import com.recred.backend.application.InviteCollaboratorUseCase;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.TutorCollaboratorInvitation;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.TutorCollaboratorInvitationRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.application.TutorInvitationTokenService;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.mail.javamail.JavaMailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InviteCollaboratorUseCaseTest {

    private TutorRepository tutorRepository;
    private TutorCollaboratorInvitationRepository invitationRepository;
    private JavaMailSender mailSender;
    private TutorInvitationTokenService tokenService;
    private UsuarioRepository usuarioRepository;
    private InviteCollaboratorUseCase useCase;

    @BeforeEach
    void setUp() {
        tutorRepository = mock(TutorRepository.class);
        invitationRepository = mock(TutorCollaboratorInvitationRepository.class);
        mailSender = mock(JavaMailSender.class);
        tokenService = mock(TutorInvitationTokenService.class);
        usuarioRepository = mock(UsuarioRepository.class);
        useCase = new InviteCollaboratorUseCase(tutorRepository, invitationRepository, Optional.of(mailSender), tokenService, usuarioRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(useCase, "frontendUrl", "http://localhost:4200");
    }

    @Test
    void execute_successful() {
        UUID tutorPrincipalId = UUID.randomUUID();
        String email = "colab@test.com";
        UUID child1 = UUID.randomUUID();
        Set<UUID> assignedStudents = Set.of(child1);

        Tutor tutorPrincipal = new Tutor();
        tutorPrincipal.setId(tutorPrincipalId);
        tutorPrincipal.setEsColaborador(false);
        Usuario usuario = new Usuario();
        usuario.setNombre("Nombre");
        tutorPrincipal.setUsuario(usuario);
        Alumno alumno = new Alumno();
        alumno.setId(child1);
        tutorPrincipal.setHijos(Set.of(alumno));

        when(tutorRepository.findById(tutorPrincipalId)).thenReturn(Optional.of(tutorPrincipal));

        useCase.execute(tutorPrincipalId, email, assignedStudents);

        verify(invitationRepository).save(any(TutorCollaboratorInvitation.class));
    }

    @Test
    void execute_failsIfTutorIsCollaborator() {
        UUID tutorPrincipalId = UUID.randomUUID();
        String email = "colab@test.com";
        Set<UUID> assignedStudents = Set.of(UUID.randomUUID());

        Tutor tutorPrincipal = new Tutor();
        tutorPrincipal.setId(tutorPrincipalId);
        tutorPrincipal.setEsColaborador(true);

        when(tutorRepository.findById(tutorPrincipalId)).thenReturn(Optional.of(tutorPrincipal));

        assertThrows(SecurityException.class, () -> useCase.execute(tutorPrincipalId, email, assignedStudents));
    }

    @Test
    void execute_failsIfChildNotAssignedToTutor() {
        UUID tutorPrincipalId = UUID.randomUUID();
        String email = "colab@test.com";
        UUID unassignedChild = UUID.randomUUID();
        Set<UUID> assignedStudents = Set.of(unassignedChild);

        Tutor tutorPrincipal = new Tutor();
        tutorPrincipal.setId(tutorPrincipalId);
        tutorPrincipal.setEsColaborador(false);
        // Tutor has no children

        when(tutorRepository.findById(tutorPrincipalId)).thenReturn(Optional.of(tutorPrincipal));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(tutorPrincipalId, email, assignedStudents));
    }
}
