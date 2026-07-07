package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetSuggestionByNotificationId;
import com.recred.backend.application.GetSuggestionByNotificationIdRequest;
import com.recred.backend.application.GetSuggestionByType;
import com.recred.backend.application.GetSuggestionByTypeRequest;
import com.recred.backend.application.GetSuggestionResponse;
import com.recred.backend.config.GlobalExceptionHandler;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.infrastructure.web.PlanAccessGuard;
import com.recred.backend.infrastructure.web.SuggestionController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.security.core.Authentication;

class SuggestionControllerTest {

    private GetSuggestionByNotificationId getSuggestionByNotificationId;
    private GetSuggestionByType getSuggestionByType;
    private PlanAccessGuard planAccessGuard;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        getSuggestionByNotificationId = mock(GetSuggestionByNotificationId.class);
        getSuggestionByType = mock(GetSuggestionByType.class);
        planAccessGuard = mock(PlanAccessGuard.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SuggestionController(
                        getSuggestionByNotificationId,
                        getSuggestionByType,
                        planAccessGuard
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getSuggestionByNotificationIdReturnsSuggestion() throws Exception {
        UUID notificationId = UUID.randomUUID();
        UUID suggestionId = UUID.randomUUID();
        String cognitoSubject = "cognito-id-123";
        GetSuggestionResponse suggestion = new GetSuggestionResponse(
                suggestionId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Title", "Message", Map.of("key", "value")
        );

        when(getSuggestionByNotificationId.execute(any(GetSuggestionByNotificationIdRequest.class)))
                .thenReturn(suggestion);
        
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(cognitoSubject);

        mockMvc.perform(get("/api/v1/sugerencias/por-notificacion/{idNotificacion}", notificationId)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(suggestionId.toString()))
                .andExpect(jsonPath("$.titulo").value("Title"))
                .andExpect(jsonPath("$.mensaje").value("Message"))
                .andExpect(jsonPath("$.metadata.key").value("value"));
    }

    @Test
    void getSuggestionsByBuffetIdAndTypeReturnsList() throws Exception {
        UUID suggestionId = UUID.randomUUID();
        String cognitoSubject = "cognito-id-123";
        GetSuggestionResponse suggestion = new GetSuggestionResponse(
                suggestionId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Title", "Message", Map.of()
        );

        when(getSuggestionByType.execute(any(GetSuggestionByTypeRequest.class)))
                .thenReturn(List.of(suggestion));

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(cognitoSubject);

        mockMvc.perform(get("/api/v1/sugerencias/agregar-producto")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(suggestionId.toString()))
                .andExpect(jsonPath("$[0].titulo").value("Title"));
        verify(planAccessGuard).requireVendorAdvanced(authentication);
    }
}
