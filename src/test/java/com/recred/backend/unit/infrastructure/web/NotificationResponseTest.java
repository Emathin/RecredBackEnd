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

    private final UUID ID = UUID.randomUUID();
    private final UUID STUDENT_ID = UUID.randomUUID();
    private final UUID PRODUCT_ID = UUID.randomUUID();
    private final UUID PURCHASE_ID = UUID.randomUUID();
    private final UUID SUGGESTION_ID = UUID.randomUUID();
    private final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 7, 4, 10, 0);
    private final LocalDateTime READ_AT = LocalDateTime.of(2026, 7, 4, 11, 0);

    @Test
    void fromDomainShouldMapAllFields() {
        Notification notification = givenNotification();
        
        NotificationResponse response = whenMappingFromDomain(notification);
        
        thenAllFieldsAreMapped(response, true);
    }

    @Test
    void fromApplicationShouldMapAllFields() {
        UserNotificationResponse applicationResponse = givenApplicationResponse();
        
        NotificationResponse response = whenMappingFromApplication(applicationResponse);
        
        thenAllFieldsAreMapped(response, false);
    }

    private Notification givenNotification() {
        return new Notification(
                ID,
                TipoNotificacion.SISTEMA,
                "Titulo",
                "Mensaje",
                true,
                CREATED_AT,
                READ_AT,
                STUDENT_ID,
                PRODUCT_ID,
                PURCHASE_ID,
                SUGGESTION_ID
        );
    }

    private UserNotificationResponse givenApplicationResponse() {
        return new UserNotificationResponse(
                ID,
                TipoNotificacion.SISTEMA,
                "Titulo",
                "Mensaje",
                false,
                CREATED_AT,
                READ_AT,
                STUDENT_ID,
                PRODUCT_ID,
                PURCHASE_ID,
                SUGGESTION_ID
        );
    }

    private NotificationResponse whenMappingFromDomain(Notification notification) {
        return NotificationResponse.fromDomain(notification);
    }

    private NotificationResponse whenMappingFromApplication(UserNotificationResponse response) {
        return NotificationResponse.fromApplication(response);
    }

    private void thenAllFieldsAreMapped(NotificationResponse response, boolean expectedReadStatus) {
        assertEquals(ID, response.id());
        assertEquals(STUDENT_ID, response.studentId());
        assertEquals(PRODUCT_ID, response.productId());
        assertEquals(PURCHASE_ID, response.purchaseId());
        assertEquals(SUGGESTION_ID, response.suggestionId());
        assertEquals(TipoNotificacion.SISTEMA, response.type());
        assertEquals("Titulo", response.title());
        assertEquals("Mensaje", response.message());
        assertEquals(expectedReadStatus, response.read());
        assertEquals(CREATED_AT, response.createdAt());
        assertEquals(READ_AT, response.readAt());
    }
}
