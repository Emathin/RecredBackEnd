package com.recred.backend.infrastructure.web;

import com.recred.backend.application.AcceptCollaboratorInvitationUseCase;
import com.recred.backend.application.InviteCollaboratorUseCase;
import com.recred.backend.application.ListCollaboratorsUseCase;
import com.recred.backend.application.RemoveCollaboratorUseCase;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.services.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tutores/colaboradores")
@RequiredArgsConstructor
public class TutorCollaboratorController {

    private final InviteCollaboratorUseCase inviteCollaboratorUseCase;
    private final AcceptCollaboratorInvitationUseCase acceptCollaboratorInvitationUseCase;
    private final ListCollaboratorsUseCase listCollaboratorsUseCase;
    private final RemoveCollaboratorUseCase removeCollaboratorUseCase;
    private final UsuarioService usuarioService;

    @PostMapping("/invitar")
    public ResponseEntity<Void> inviteCollaborator(Authentication authentication, @RequestBody InviteCollaboratorRequest request) {
        Tutor tutor = getTutorActual(authentication);
        inviteCollaboratorUseCase.execute(tutor.getId(), request.email(), request.assignedStudentIds());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/aceptar")
    public ResponseEntity<Void> acceptInvitation(@RequestBody AcceptCollaboratorRequest request) {
        // This endpoint can be public depending on the security config, but it's used for registration
        acceptCollaboratorInvitationUseCase.execute(
                request.token(),
                request.nombre(),
                request.apellido(),
                request.password(),
                request.dni(),
                request.telefono()
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<TutorResponseDTO>> listCollaborators(Authentication authentication) {
        Tutor tutor = getTutorActual(authentication);
        List<Tutor> collaborators = listCollaboratorsUseCase.execute(tutor.getId());
        List<TutorResponseDTO> response = collaborators.stream()
                .map(colab -> new TutorResponseDTO(
                        colab.getId(),
                        colab.getUsuario().getNombre(),
                        colab.getUsuario().getApellido(),
                        colab.getUsuario().getEmail(),
                        colab.getActiva()
                ))
                .toList();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{colaboradorId}")
    public ResponseEntity<Void> removeCollaborator(Authentication authentication, @PathVariable UUID colaboradorId) {
        Tutor tutor = getTutorActual(authentication);
        removeCollaboratorUseCase.execute(tutor.getId(), colaboradorId);
        return ResponseEntity.noContent().build();
    }

    private Tutor getTutorActual(Authentication authentication) {
        String cognitoSub = Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Usuario no autenticado"));
        Usuario usuario = usuarioService.obtenerUsuarioActual(cognitoSub);
        if (usuario.getTutor() == null) {
            throw new SecurityException("El usuario no es un tutor");
        }
        return usuario.getTutor();
    }
}

record InviteCollaboratorRequest(String email, Set<UUID> assignedStudentIds) {}
record AcceptCollaboratorRequest(String token, String nombre, String apellido, String password, String dni, String telefono) {}
record TutorResponseDTO(UUID id, String nombre, String apellido, String email, Boolean activa) {}
