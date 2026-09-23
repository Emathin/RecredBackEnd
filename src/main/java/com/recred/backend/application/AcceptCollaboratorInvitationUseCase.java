package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.TutorCollaboratorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.TutorCollaboratorInvitationRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.CognitoTutorIdentityService;
import com.recred.backend.domain.services.CreatedCognitoUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcceptCollaboratorInvitationUseCase {

    private final TutorCollaboratorInvitationRepository invitationRepository;
    private final TutorRepository tutorRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final CognitoTutorIdentityService cognitoIdentityService;
    private final TutorInvitationTokenService tokenService;

    @Transactional
    public void execute(String rawToken, String nombre, String apellido, String password, String dni, String telefono) {
        String tokenHash = tokenService.hashToken(rawToken);

        TutorCollaboratorInvitation invitation = invitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invitación inválida o no encontrada"));

        if (invitation.isExpired() || invitation.getStatus() != TutorInvitationStatus.PENDING) {
            throw new IllegalStateException("La invitación ha expirado o ya no es válida");
        }

        Tutor tutorPrincipal = tutorRepository.findById(invitation.getTutorPrincipalId())
                .orElseThrow(() -> new IllegalStateException("Tutor principal no encontrado"));

        CreatedCognitoUser cognitoUser = cognitoIdentityService.createTutorUser(
                invitation.getEmail(), invitation.getEmail(), nombre, apellido);
        // Note: The password usually is set via the Cognito service, or the user signs up with password themselves.
        // If the service doesn't set password, we assume it sends a temporary password email.
        
        Usuario newUser = new Usuario();
        newUser.setCognitoSub(cognitoUser.sub());
        newUser.setEmail(invitation.getEmail());
        newUser.setNombre(nombre);
        newUser.setApellido(apellido);
        newUser.setDni(dni);
        newUser.setTelefono(telefono);
        newUser.setRol("PADRE"); // El colaborador también tiene rol padre
        newUser.setTipoPlan(tutorPrincipal.getUsuario().getTipoPlan());
        newUser.setAutenticadoConCognito(true);
        newUser.setActivo(true);
        usuarioRepository.save(newUser);

        Tutor colaborador = new Tutor();
        colaborador.setUsuario(newUser);
        colaborador.setEsColaborador(true);
        colaborador.setTutorPrincipalId(tutorPrincipal.getId());
        
        tutorRepository.save(colaborador);

        // Assign to the selected students
        for (UUID studentId : invitation.getAssignedStudentIds()) {
            Alumno alumno = alumnoRepository.findById(studentId).orElse(null);
            if (alumno != null) {
                colaborador.addHijo(alumno);
            }
        }
        
        tutorRepository.save(colaborador);

        invitation.setStatus(TutorInvitationStatus.ACCEPTED);
        invitation.setUsadaEn(LocalDateTime.now());
        invitationRepository.save(invitation);
    }
}
