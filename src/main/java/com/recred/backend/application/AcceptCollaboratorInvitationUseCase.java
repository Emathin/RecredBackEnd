package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.RolUsuario;
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
    public void execute(String rawToken, String nombre, String apellido, 
                        String password, String dni, String telefono) {
        String tokenHash = tokenService.hash(rawToken);

        TutorCollaboratorInvitation invitation = invitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invitación inválida o no encontrada"));

        if (invitation.isExpired() || invitation.getStatus() != TutorInvitationStatus.PENDING) {
            throw new IllegalStateException("La invitación ha expirado o ya no es válida");
        }

        if (invitation.getTutorPrincipalId() == null) {
            throw new IllegalStateException("El ID del tutor principal es nulo en la invitación");
        }
        Tutor tutorPrincipal = tutorRepository.findById(invitation.getTutorPrincipalId())
                .orElseThrow(() -> new IllegalStateException("Tutor principal no encontrado"));

        CreatedCognitoUser cognitoUser = cognitoIdentityService.createTutorUser(
                null, invitation.getEmail(), nombre, apellido);
        
        if (password != null && !password.isBlank()) {
            cognitoIdentityService.setPermanentPassword(cognitoUser.username(), password);
        }
        
        Usuario newUser = new Usuario();
        newUser.setIdCognito(cognitoUser.sub());
        newUser.setEmail(invitation.getEmail());
        newUser.setNombre(nombre);
        newUser.setApellido(apellido);
        newUser.setDni(dni);
        newUser.setTelefono(telefono);
        newUser.setRol(RolUsuario.PADRE); // El colaborador también tiene rol padre
        newUser.setPlan(tutorPrincipal.getUsuario().getPlan());
        newUser.setAutenticadoConCognito(true);
        newUser.setActivo(true);
        Tutor colaborador = new Tutor();
        colaborador.setActiva(true);
        colaborador.setEsColaborador(true);
        colaborador.setTutorPrincipalId(tutorPrincipal.getId());

        // Assign to the selected students
        if (invitation.getAssignedStudentIds() != null) {
            for (UUID studentId : invitation.getAssignedStudentIds()) {
                if (studentId == null) {
                    continue;
                }
                Alumno alumno = alumnoRepository.findById(studentId).orElse(null);
                if (alumno != null) {
                    colaborador.addHijo(alumno);
                }
            }
        }
        
        newUser.asignarTutor(colaborador);
        
        // This save will persist both the new Usuario and the cascaded Tutor
        usuarioRepository.save(newUser);

        invitation.setStatus(TutorInvitationStatus.ACCEPTED);
        invitation.setUsadaEn(LocalDateTime.now());
        invitationRepository.save(invitation);
    }
}
