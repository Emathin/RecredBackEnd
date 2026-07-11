package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetSuggestionByNotificationId;
import com.recred.backend.application.GetSuggestionByNotificationIdRequest;
import com.recred.backend.application.GetSuggestionByType;
import com.recred.backend.application.GetSuggestionByTypeRequest;
import com.recred.backend.application.GetSuggestionResponse;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.infrastructure.web.PlanAccessGuard;
import com.recred.backend.infrastructure.web.SuggestionController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuggestionControllerTest {

    @Mock private GetSuggestionByNotificationId getSuggestionByNotificationId;
    @Mock private GetSuggestionByType getSuggestionByType;
    @Mock private PlanAccessGuard planAccessGuard;
    
    private SuggestionController controller;
    
    private final UUID suggestionId = UUID.randomUUID();
    private final String cognitoSubject = "cognito-id-123";

    @BeforeEach
    void setUp() {
        controller = new SuggestionController(
                getSuggestionByNotificationId,
                getSuggestionByType,
                planAccessGuard
        );
    }

    @Test
    void givenNotificationIdWhenGettingSuggestionThenReturnsSuggestion() {
        UUID notificationId = UUID.randomUUID();
        GetSuggestionResponse suggestion = givenSuggestion();
        givenSuggestionByNotificationIdReturns(suggestion);
        
        Authentication authentication = givenAuthentication();

        ResponseEntity<GetSuggestionResponse> response = whenGettingSuggestionByNotificationId(authentication, notificationId);

        thenSuggestionIsReturned(response, suggestionId, "Title", "Message", "value");
    }

    @Test
    void givenTypeWhenGettingSuggestionsThenReturnsSuggestionListAndGuardsAccess() {
        GetSuggestionResponse suggestion = givenSuggestion();
        givenSuggestionByTypeReturns(suggestion);

        Authentication authentication = givenAuthentication();

        ResponseEntity<List<GetSuggestionResponse>> response = whenGettingSuggestionByType(authentication);

        thenSuggestionListIsReturned(response, suggestionId, "Title");
        thenVendorAdvancedAccessWasRequired(authentication);
    }

    private GetSuggestionResponse givenSuggestion() {
        return new GetSuggestionResponse(
                suggestionId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Title", "Message", Map.of("key", "value")
        );
    }

    private void givenSuggestionByNotificationIdReturns(GetSuggestionResponse suggestion) {
        when(getSuggestionByNotificationId.execute(any(GetSuggestionByNotificationIdRequest.class)))
                .thenReturn(suggestion);
    }

    private void givenSuggestionByTypeReturns(GetSuggestionResponse suggestion) {
        when(getSuggestionByType.execute(any(GetSuggestionByTypeRequest.class)))
                .thenReturn(List.of(suggestion));
    }

    private Authentication givenAuthentication() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(cognitoSubject);
        return authentication;
    }

    private ResponseEntity<GetSuggestionResponse> whenGettingSuggestionByNotificationId(
            Authentication authentication, UUID notificationId) {
        return controller.getSuggestionByNotificationId(authentication, notificationId);
    }

    private ResponseEntity<List<GetSuggestionResponse>> whenGettingSuggestionByType(
            Authentication authentication) {
        return controller.getSuggestionsByBuffetIdAndType(authentication);
    }

    private void thenSuggestionIsReturned(
            ResponseEntity<GetSuggestionResponse> response, UUID expectedId, String title, String msg, String metaValue) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedId, response.getBody().id());
        assertEquals(title, response.getBody().titulo());
        assertEquals(msg, response.getBody().mensaje());
        assertEquals(metaValue, response.getBody().metadata().get("key"));
    }

    private void thenSuggestionListIsReturned(
            ResponseEntity<List<GetSuggestionResponse>> response, UUID expectedId, String title) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(expectedId, response.getBody().get(0).id());
        assertEquals(title, response.getBody().get(0).titulo());
    }

    private void thenVendorAdvancedAccessWasRequired(Authentication authentication) {
        verify(planAccessGuard).requireVendorAdvanced(authentication);
    }
}
