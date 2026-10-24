package com.recred.backend.application;

import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.ExternalPaymentService;
import org.springframework.stereotype.Service;

@Service
public class ScheduleWalletTopUp {

    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final ExternalPaymentService externalPaymentService;

    public ScheduleWalletTopUp(
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository,
            ExternalPaymentService externalPaymentService) {
        this.usuarioRepository = usuarioRepository;
        this.tutorRepository = tutorRepository;
        this.externalPaymentService = externalPaymentService;
    }

    public ScheduleWalletTopUpResponse execute(
            String cognitoSub, ScheduleWalletTopUpRequest request) {
        request.validate();
        
        Usuario parentUser = usuarioRepository.findByIdCognito(cognitoSub)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));
                
        parentUser.validateCanLoadBalanceToChild();
        
        Tutor tutor = tutorRepository.findByUsuarioId(parentUser.getId())
                .orElseThrow(() -> new SecurityException("El usuario no es un tutor válido"));
                
        tutor.childOrThrow(request.studentId(), "El alumno no es hijo del tutor o no existe");

        String preapprovalUrl;
        if (request.startDate() != null) {
            preapprovalUrl = externalPaymentService.createScheduledTopUpPreapproval(
                    request.studentId(),
                    request.amount(),
                    request.frequency(),
                    request.frequencyType(),
                    parentUser.getEmail(),
                    request.startDate()
            );
        } else {
            preapprovalUrl = externalPaymentService.createScheduledTopUpPreapproval(
                    request.studentId(),
                    request.amount(),
                    request.frequency(),
                    request.frequencyType(),
                    parentUser.getEmail()
            );
        }

        return new ScheduleWalletTopUpResponse(preapprovalUrl);
    }
}
