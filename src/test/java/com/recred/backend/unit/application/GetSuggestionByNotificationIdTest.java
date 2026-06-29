package com.recred.backend.unit.application;

import com.recred.backend.application.GetSuggestionByNotificationId;
import com.recred.backend.application.GetSuggestionByNotificationIdRequest;
import com.recred.backend.application.GetSuggestionResponse;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.repositories.SuggestionAiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSuggestionByNotificationIdTest {

    @Mock
    private SuggestionAiRepository suggestionAiRepository;

    private GetSuggestionByNotificationId useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetSuggestionByNotificationId(suggestionAiRepository);
    }

    @Test
    void executeShouldReturnSuggestion() {
        UUID notificationId = UUID.randomUUID();
        String cognitoSubject = "cognito-id-123";
        SugerenciaIA suggestion = SugerenciaIA.builder().id(UUID.randomUUID()).build();
        
        when(suggestionAiRepository.getSuggestionByNotificationId(notificationId))
                .thenReturn(suggestion);

        GetSuggestionByNotificationIdRequest request = new GetSuggestionByNotificationIdRequest(
                notificationId, cognitoSubject);
        GetSuggestionResponse result = useCase.execute(request);

        assertNotNull(result);
        assertEquals(suggestion.getId(), result.id());
    }
}
