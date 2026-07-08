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

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSuggestionByNotificationIdTest {

    private static final String COGNITO_SUBJECT = "cognito-id-123";

    @Mock
    private SuggestionAiRepository suggestionAiRepository;

    private GetSuggestionByNotificationId useCase;
    private UUID notificationId;
    private UUID suggestionId;

    @BeforeEach
    void setUp() {
        useCase = new GetSuggestionByNotificationId(suggestionAiRepository);
    }

    @Test
    void givenSuggestionExistsForNotification_whenGettingSuggestion_thenReturnsSuggestion() {
        givenSuggestionExistsForNotification();

        GetSuggestionResponse response = whenGetSuggestionByNotificationId();

        thenSuggestionWasReturned(response);
    }

    @Test
    void givenSuggestionDoesNotExistForNotification_whenGettingSuggestion_thenReturnsNull() {
        givenSuggestionDoesNotExistForNotification();

        GetSuggestionResponse response = whenGetSuggestionByNotificationId();

        thenNoSuggestionWasReturned(response);
    }

    private void givenSuggestionExistsForNotification() {
        notificationId = randomUUID();
        suggestionId = randomUUID();
        when(suggestionAiRepository.getSuggestionByNotificationId(notificationId))
                .thenReturn(SugerenciaIA.builder().id(suggestionId).build());
    }

    private void givenSuggestionDoesNotExistForNotification() {
        notificationId = randomUUID();
        when(suggestionAiRepository.getSuggestionByNotificationId(notificationId))
                .thenReturn(null);
    }

    private GetSuggestionResponse whenGetSuggestionByNotificationId() {
        return useCase.execute(new GetSuggestionByNotificationIdRequest(
                notificationId,
                COGNITO_SUBJECT
        ));
    }

    private void thenSuggestionWasReturned(GetSuggestionResponse response) {
        assertEquals(suggestionId, response.id());
    }

    private void thenNoSuggestionWasReturned(GetSuggestionResponse response) {
        assertNull(response);
    }

}
