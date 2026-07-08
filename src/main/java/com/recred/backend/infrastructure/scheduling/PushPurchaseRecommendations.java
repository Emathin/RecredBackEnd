package com.recred.backend.infrastructure.scheduling;

import com.recred.backend.application.GeneratePurchaseRecommendation;
import com.recred.backend.application.GeneratePurchaseRecommendationRequest;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.repositories.SuggestionAiRepository;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.push.PushNotificationRequestedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class PushPurchaseRecommendations {

    private final GeneratePurchaseRecommendation generatePurchaseRecommendation;
    private final SuggestionAiRepository suggestionAiRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PushPurchaseRecommendations(
            GeneratePurchaseRecommendation generatePurchaseRecommendation,
            SuggestionAiRepository suggestionAiRepository,
            ApplicationEventPublisher eventPublisher) {
        this.generatePurchaseRecommendation = generatePurchaseRecommendation;
        this.suggestionAiRepository = suggestionAiRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void pushNotificationForTomorrowPurchase() {
        String tomorrow = LocalDate.now().plusDays(1).getDayOfWeek().name();
        List<NotificacionEntity> notificationsToPush = suggestionAiRepository.findByTipo(tomorrow);
        for (NotificacionEntity notification : notificationsToPush) {
            eventPublisher.publishEvent(new PushNotificationRequestedEvent(new PushNotification(
                    notification.getUsuarioEntity().getId(),
                    notification.getTitulo(),
                    notification.getMensaje(),
                    pushData(notification)
            )));
            generatePurchaseRecommendation.execute(new GeneratePurchaseRecommendationRequest());
        }
    }

    private Map<String, String> pushData(NotificacionEntity notification) {
        Map<String, String> data = new LinkedHashMap<>();
        putIfPresent(data, "notificacionId", notification.getId());
        putIfPresent(data, "alumnoId", notification.getAlumnoEntity().getId());
        putIfPresent(data, "productoId", notification.getProductoEntity().getId());
        putIfPresent(data, "sugerenciaIaId", notification.getSugerenciaIaEntity().getId());
        data.put("tipo", notification.getTipo().name());
        return data;
    }

    private void putIfPresent(Map<String, String> data, String key, UUID value) {
        if (value != null) {
            data.put(key, value.toString());
        }
    }
}
