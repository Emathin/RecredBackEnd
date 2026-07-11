package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetLoggedUserNotifications;
import com.recred.backend.application.GetLoggedUserNotificationsRequest;
import com.recred.backend.application.GetLoggedUserNotificationsResponse;
import com.recred.backend.application.SetNotificationAsAlreadyRead;
import com.recred.backend.application.SetNotificationAsAlreadyReadRequest;
import com.recred.backend.application.UserNotificationResponse;
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
import org.springframework.test.web.servlet.ResultActions;
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

    private final UUID NOTIFICATION_ID = UUID.randomUUID();
    private final String COGNITO_SUB = "cognito-sub";

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
    void getMyNotificationsShouldReturnAuthenticatedUserNotifications() throws Exception {
        givenNotifications();

        ResultActions response = whenGettingNotifications();

        thenNotificationsAreReturned(response);
    }

    @Test
    void markAsReadShouldReturnOk() throws Exception {
        ResultActions response = whenMarkingAsRead();

        thenMarkAsReadSucceeds(response);
    }

    @Test
    void markAsReadWhenNotFoundShouldReturn404() throws Exception {
        givenNotificationNotFound();

        ResultActions response = whenMarkingAsRead();

        thenReturnsNotFound(response);
    }

    private void givenNotifications() {
        when(getLoggedUserNotifications.execute(new GetLoggedUserNotificationsRequest(
                COGNITO_SUB,
                true,
                1,
                5
        ))).thenReturn(new GetLoggedUserNotificationsResponse(List.of(
                UserNotificationResponse.fromDomain(notification(NOTIFICATION_ID))
        )));
    }

    private void givenNotificationNotFound() {
        doThrow(new NotificationNotFoundException("Not found"))
                .when(setNotificationAsAlreadyRead).execute(
                        new SetNotificationAsAlreadyReadRequest(NOTIFICATION_ID));
    }

    private ResultActions whenGettingNotifications() throws Exception {
        return mockMvc.perform(get("/api/v1/notifications/me")
                .principal(authentication())
                .param("unreadOnly", "true")
                .param("page", "1")
                .param("size", "5"));
    }

    private ResultActions whenMarkingAsRead() throws Exception {
        return mockMvc.perform(put("/api/v1/notifications/{id}/already-read", NOTIFICATION_ID)
                .principal(authentication()));
    }

    private void thenNotificationsAreReturned(ResultActions response) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(NOTIFICATION_ID.toString()))
                .andExpect(jsonPath("$[0].type").value("SISTEMA"))
                .andExpect(jsonPath("$[0].title").value("Title"))
                .andExpect(jsonPath("$[0].message").value("Message"))
                .andExpect(jsonPath("$[0].read").value(false))
                .andExpect(jsonPath("$[0].createdAt").value("2026-06-02T14:30:00"))
                .andExpect(jsonPath("$[0].readAt").doesNotExist());

        verify(getLoggedUserNotifications).execute(new GetLoggedUserNotificationsRequest(
                COGNITO_SUB,
                true,
                1,
                5
        ));
    }

    private void thenMarkAsReadSucceeds(ResultActions response) throws Exception {
        response.andExpect(status().isOk());
        verify(setNotificationAsAlreadyRead).execute(new SetNotificationAsAlreadyReadRequest(
                NOTIFICATION_ID
        ));
    }

    private void thenReturnsNotFound(ResultActions response) throws Exception {
        response.andExpect(status().isNotFound());
    }

    private Notification notification(UUID notificationId) {
        return new Notification(
                notificationId,
                TipoNotificacion.SISTEMA,
                "Title",
                "Message",
                false,
                LocalDateTime.of(2026, 6, 2, 14, 30),
                null,
                null,
                null,
                null,
                null
        );
    }

    private Authentication authentication() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(COGNITO_SUB)
                .issuedAt(Instant.parse("2026-06-02T14:00:00Z"))
                .expiresAt(Instant.parse("2026-06-02T15:00:00Z"))
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
