package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetLoggedUserNotifications;
import com.recred.backend.application.GetLoggedUserNotificationsResponse;
import com.recred.backend.application.SetNotificationAsAlreadyRead;
import com.recred.backend.config.GlobalExceptionHandler;
import com.recred.backend.domain.exceptions.NotificationNotFoundException;
import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.infrastructure.web.NotificationController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerTest {

    private GetLoggedUserNotifications getLoggedUserNotifications;
    private SetNotificationAsAlreadyRead setNotificationAsAlreadyRead;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        getLoggedUserNotifications = mock(GetLoggedUserNotifications.class);
        setNotificationAsAlreadyRead = mock(SetNotificationAsAlreadyRead.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NotificationController(getLoggedUserNotifications, setNotificationAsAlreadyRead))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getMyNotificationsReturnsAuthenticatedUserNotifications() throws Exception {
        UUID notificationId = UUID.randomUUID();
        when(getLoggedUserNotifications.execute("cognito-sub", true, 1, 5))
                .thenReturn(new GetLoggedUserNotificationsResponse(List.of(notificationDTO(notificationId))));

        mockMvc.perform(get("/api/v1/notifications/me")
                        .principal(authentication())
                        .param("unreadOnly", "true")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications[0].id").value(notificationId.toString()))
                .andExpect(jsonPath("$.notifications[0].type").value("SISTEMA"))
                .andExpect(jsonPath("$.notifications[0].title").value("Title"))
                .andExpect(jsonPath("$.notifications[0].message").value("Message"))
                .andExpect(jsonPath("$.notifications[0].read").value(false))
                .andExpect(jsonPath("$.notifications[0].createdAt").value("2026-06-02T14:30:00"))
                .andExpect(jsonPath("$.notifications[0].readAt").doesNotExist());

        verify(getLoggedUserNotifications).execute("cognito-sub", true, 1, 5);
    }

    @Test
    void markAsReadSuccess() throws Exception {
        UUID notificationId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/notifications/{id}/already-read", notificationId)
                        .principal(authentication()))
                .andExpect(status().isOk());

        verify(setNotificationAsAlreadyRead).execute(notificationId);
    }

    @Test
    void markAsReadNotFoundReturns404() throws Exception {
        UUID notificationId = UUID.randomUUID();
        doThrow(new NotificationNotFoundException("Not found"))
                .when(setNotificationAsAlreadyRead).execute(notificationId);

        mockMvc.perform(put("/api/v1/notifications/{id}/already-read", notificationId)
                        .principal(authentication()))
                .andExpect(status().isNotFound());
    }

    private GetLoggedUserNotificationsResponse.NotificationDTO notificationDTO(UUID notificationId) {
        return new GetLoggedUserNotificationsResponse.NotificationDTO(
                notificationId,
                TipoNotificacion.SISTEMA,
                "Title",
                "Message",
                false,
                LocalDateTime.of(2026, 6, 2, 14, 30),
                null
        );
    }

    private Authentication authentication() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("cognito-sub")
                .issuedAt(Instant.parse("2026-06-02T14:00:00Z"))
                .expiresAt(Instant.parse("2026-06-02T15:00:00Z"))
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}

