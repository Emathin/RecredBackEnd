package com.recred.backend.web;

import com.recred.backend.application.GetSuggestionByNotificationId;
import com.recred.backend.config.GlobalExceptionHandler;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.infrastructure.web.SuggestionController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SuggestionControllerTest {

    private GetSuggestionByNotificationId getSuggestionByNotificationId;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        getSuggestionByNotificationId = mock(GetSuggestionByNotificationId.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SuggestionController(getSuggestionByNotificationId))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getSuggestionByNotificationIdReturnsSuggestion() throws Exception {
        UUID notificationId = UUID.randomUUID();
        UUID suggestionId = UUID.randomUUID();
        SugerenciaIA suggestion = SugerenciaIA.builder()
                .id(suggestionId)
                .alumnoId(UUID.randomUUID())
                .titulo("Title")
                .mensaje("Message")
                .metadata(Map.of("key", "value"))
                .build();

        when(getSuggestionByNotificationId.execute(notificationId)).thenReturn(suggestion);

        mockMvc.perform(get("/api/v1/sugerencias/por-notificacion/{idNotificacion}", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(suggestionId.toString()))
                .andExpect(jsonPath("$.titulo").value("Title"))
                .andExpect(jsonPath("$.mensaje").value("Message"))
                .andExpect(jsonPath("$.metadata.key").value("value"));
    }
}
