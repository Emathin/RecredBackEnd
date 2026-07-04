package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.UserNotificationResponse;
import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.infrastructure.web.NotificationResponse;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationResponseTest {

    @Test
    void fromDomainShouldMapAllFields() {
        UUID id = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID purchaseId = UUID.randomUUID();
        UUID suggestionId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 4, 10, 0);
        LocalDateTime readAt = LocalDateTime.of(2026, 7, 4, 11, 0);
        Notification notification = new Notification(
                id,
                TipoNotificacion.SISTEMA,
                "Titulo",
                "Mensaje",
                true,
                createdAt,
                readAt,
                studentId,
                productId,
                purchaseId,
                suggestionId
        );

        NotificationResponse response = NotificationResponse.fromDomain(notification);

        assertResponse(response, id, studentId, productId, purchaseId, suggestionId);
        assertEquals(TipoNotificacion.SISTEMA, response.type());
        assertEquals("Titulo", response.title());
        assertEquals("Mensaje", response.message());
        assertEquals(true, response.read());
        assertEquals(createdAt, response.createdAt());
        assertEquals(readAt, response.readAt());
    }

    @Test
    void fromApplicationShouldMapAllFields() {
        UUID id = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID purchaseId = UUID.randomUUID();
        UUID suggestionId = UUID.randomUUID();

        NotificationResponse response = NotificationResponse.fromApplication(
                new UserNotificationResponse(
                        id,
                        TipoNotificacion.SISTEMA,
                        "Titulo",
                        "Mensaje",
                        false,
                        LocalDateTime.of(2026, 7, 4, 10, 0),
                        null,
                        studentId,
                        productId,
                        purchaseId,
                        suggestionId
                )
        );

        assertResponse(response, id, studentId, productId, purchaseId, suggestionId);
        assertEquals(false, response.read());
    }

    private void assertResponse(
            NotificationResponse response,
            UUID id,
            UUID studentId,
            UUID productId,
            UUID purchaseId,
            UUID suggestionId
    ) {
        assertEquals(id, response.id());
        assertEquals(studentId, response.studentId());
        assertEquals(productId, response.productId());
        assertEquals(purchaseId, response.purchaseId());
        assertEquals(suggestionId, response.suggestionId());
    }
}
