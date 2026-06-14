package com.recred.backend.infrastructure.scheduling;

import com.recred.backend.application.GenerateParentSummary;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.services.PushNotificationSender;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.NotificacionJpaRepository;
import com.recred.backend.infrastructure.persistence.SpringDataUsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class GenerateParentSummaryJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateParentSummaryJob.class);

    private final TutorRepository tutorRepository;
    private final GenerateParentSummary generateParentSummary;
    private final PushNotificationSender pushNotificationSender;
    private final NotificacionJpaRepository notificacionJpaRepository;
    private final SpringDataUsuarioRepository usuarioRepository;

    public GenerateParentSummaryJob(
            TutorRepository tutorRepository,
            GenerateParentSummary generateParentSummary,
            PushNotificationSender pushNotificationSender,
            NotificacionJpaRepository notificacionJpaRepository,
            SpringDataUsuarioRepository usuarioRepository
    ) {
        this.tutorRepository = tutorRepository;
        this.generateParentSummary = generateParentSummary;
        this.pushNotificationSender = pushNotificationSender;
        this.notificacionJpaRepository = notificacionJpaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Scheduled(cron = "00 34 18 * * SUN")
    public void run() {
        List<Tutor> tutors = tutorRepository.findAll();
        LocalDateTime fin = LocalDateTime.now();
        LocalDateTime inicio = fin.minusDays(7);

        for (Tutor tutor : tutors) {
            try {
                generateParentSummary.execute(tutor.getUsuario().getId(), inicio, fin);

                PushNotification notification = new PushNotification(
                        tutor.getUsuario().getId(),
                        "Resumen Semanal",
                        "Tu resumen semanal de gastos ya está disponible.",
                        Map.of("type", "RESUMEN_SEMANAL")
                );
                pushNotificationSender.send(notification);

                UsuarioEntity usuario = usuarioRepository.findById(tutor.getUsuario().getId())
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

                NotificacionEntity notificacionEntity = new NotificacionEntity();
                notificacionEntity.setUsuarioEntity(usuario);
                notificacionEntity.setTipo(TipoNotificacion.RESUMEN_SEMANAL);
                notificacionEntity.setTitulo("Resumen Semanal");
                notificacionEntity.setMensaje("Tu resumen semanal de gastos ya está disponible.");
                notificacionEntity.setLeida(false);
                notificacionEntity.setCreadoEn(LocalDateTime.now());

                notificacionJpaRepository.save(notificacionEntity);

            } catch (Exception e) {
                LOGGER.error("Error al generar resumen o enviar notificación para el tutor {}: {}",
                        tutor.getUsuario().getId(), e.getMessage(), e);
            }
        }
    }
}
