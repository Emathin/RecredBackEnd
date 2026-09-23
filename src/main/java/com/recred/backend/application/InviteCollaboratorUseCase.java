package com.recred.backend.application;

import com.recred.backend.domain.exceptions.TutorNoEncontradoException;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.TutorCollaboratorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;
import com.recred.backend.domain.repositories.TutorCollaboratorInvitationRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InviteCollaboratorUseCase {

    private final TutorRepository tutorRepository;
    private final TutorCollaboratorInvitationRepository invitationRepository;
    private final JavaMailSender mailSender;
    private final TutorInvitationTokenService tokenService;

    @Value("${spring.mail.username:no-reply@recred.com}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Transactional
    public void execute(UUID tutorId, String email, Set<UUID> assignedStudentIds) {
        Tutor tutor = tutorRepository.findById(tutorId)
                .orElseThrow(() -> new TutorNoEncontradoException("Tutor principal no encontrado"));

        if (tutor.getEsColaborador()) {
            throw new SecurityException("Los colaboradores no pueden invitar a otros colaboradores");
        }

        // Validar que los assignedStudentIds pertenezcan realmente al tutor principal
        for (UUID studentId : assignedStudentIds) {
            boolean ownsStudent = tutor.getHijos().stream()
                    .anyMatch(hijo -> hijo.getId().equals(studentId));
            if (!ownsStudent) {
                throw new IllegalArgumentException("No puedes asignar a un colaborador un hijo que no te pertenece");
            }
        }

        String rawToken = tokenService.generateToken();
        String tokenHash = tokenService.hashToken(rawToken);

        TutorCollaboratorInvitation invitation = new TutorCollaboratorInvitation();
        invitation.setTutorPrincipalId(tutor.getId());
        invitation.setEmail(email);
        invitation.setTokenHash(tokenHash);
        invitation.setStatus(TutorInvitationStatus.PENDING);
        invitation.setExpiraEn(LocalDateTime.now().plusDays(7));
        invitation.setAssignedStudentIds(assignedStudentIds);

        invitationRepository.save(invitation);

        String invitationLink = frontendUrl + "/colaborador/aceptar-invitacion/" + rawToken;
        sendEmail(email, tutor.getUsuario().getNombre(), invitationLink);
    }

    private void sendEmail(String to, String inviterName, String link) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Invitación para colaborar en Recred");
        message.setText("Hola,\n\n" +
                inviterName + " te ha invitado a colaborar y aportar saldo a las cuentas de sus hijos en Recred.\n\n" +
                "Para aceptar la invitación y registrarte, haz clic en el siguiente enlace:\n" +
                link + "\n\n" +
                "Este enlace expira en 7 días.\n\nSaludos,\nEl equipo de Recred");
        mailSender.send(message);
    }
}
