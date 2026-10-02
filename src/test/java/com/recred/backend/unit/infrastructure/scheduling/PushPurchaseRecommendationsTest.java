package com.recred.backend.unit.infrastructure.scheduling;

import com.recred.backend.infrastructure.scheduling.*;

import com.recred.backend.application.GeneratePurchaseRecommendation;
import com.recred.backend.application.GeneratePurchaseRecommendationRequest;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.domain.repositories.SuggestionAiRepository;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.push.PushNotificationRequestedEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PushPurchaseRecommendationsTest {

    private GeneratePurchaseRecommendation generatePurchaseRecommendation;
    private SuggestionAiRepository suggestionAiRepository;
    private ApplicationEventPublisher eventPublisher;
    private PushPurchaseRecommendations scheduler;

    @BeforeEach
    void setUp() {
        generatePurchaseRecommendation = mock(GeneratePurchaseRecommendation.class);
        suggestionAiRepository = mock(SuggestionAiRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        scheduler = new PushPurchaseRecommendations(
                generatePurchaseRecommendation,
                suggestionAiRepository,
                eventPublisher
        );
    }

    @Test
    void pushNotificationForTomorrowPurchasePublishesPushRequestForEachNotification() {
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID suggestionId = UUID.randomUUID();
        String tomorrow = LocalDate.now().plusDays(1).getDayOfWeek().name();
        NotificacionEntity notification = notification(
                notificationId,
                userId,
                studentId,
                productId,
                suggestionId
        );
        when(suggestionAiRepository.findByTipo(tomorrow)).thenReturn(List.of(notification));

        scheduler.pushNotificationForTomorrowPurchase();

        ArgumentCaptor<PushNotificationRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(PushNotificationRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(userId, eventCaptor.getValue().notification().userId());
        assertEquals("Titulo", eventCaptor.getValue().notification().title());
        assertEquals("Mensaje", eventCaptor.getValue().notification().message());
        assertEquals(notificationId.toString(),
                eventCaptor.getValue().notification().data().get("notificacionId"));
        assertEquals(studentId.toString(),
                eventCaptor.getValue().notification().data().get("alumnoId"));
        assertEquals(productId.toString(),
                eventCaptor.getValue().notification().data().get("productoId"));
        assertEquals(suggestionId.toString(),
                eventCaptor.getValue().notification().data().get("sugerenciaIaId"));
        assertEquals(TipoNotificacion.SUGERENCIA_IA.name(),
                eventCaptor.getValue().notification().data().get("tipo"));
        verify(generatePurchaseRecommendation).execute(new GeneratePurchaseRecommendationRequest());
    }

    private NotificacionEntity notification(
            UUID notificationId,
            UUID userId,
            UUID studentId,
            UUID productId,
            UUID suggestionId) {
        UsuarioEntity user = new UsuarioEntity();
        user.setId(userId);

        AlumnoEntity student = new AlumnoEntity();
        student.setId(studentId);

        ProductoEntity product = new ProductoEntity();
        product.setId(productId);

        SugerenciaIAEntity suggestion = new SugerenciaIAEntity();
        suggestion.setId(suggestionId);

        NotificacionEntity notification = new NotificacionEntity();
        notification.setId(notificationId);
        notification.setUsuarioEntity(user);
        notification.setAlumnoEntity(student);
        notification.setProductoEntity(product);
        notification.setSugerenciaIaEntity(suggestion);
        notification.setTipo(TipoNotificacion.SUGERENCIA_IA);
        notification.setTitulo("Titulo");
        notification.setMensaje("Mensaje");
        return notification;
    }
}
