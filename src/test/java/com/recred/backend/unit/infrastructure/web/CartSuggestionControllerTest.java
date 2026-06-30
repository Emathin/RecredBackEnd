package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetCartSuggestions;
import com.recred.backend.application.GetCartSuggestionsRequest;
import com.recred.backend.application.GetCartSuggestionsResponse;
import com.recred.backend.infrastructure.web.CartSuggestionController;
import com.recred.backend.infrastructure.web.CartSuggestionRequestDTO;
import com.recred.backend.infrastructure.web.CartSuggestionResponseDTO;
import com.recred.backend.infrastructure.web.PurchaseItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartSuggestionControllerTest {

    private GetCartSuggestions getCartSuggestions;
    private CartSuggestionController controller;

    @BeforeEach
    void setUp() {
        getCartSuggestions = mock(GetCartSuggestions.class);
        controller = new CartSuggestionController(getCartSuggestions);
    }

    @Test
    void suggestMapsRequestItemsToUseCaseRequest() {
        UUID studentId = randomUUID();
        UUID buffetId = randomUUID();
        UUID cartProductId = randomUUID();
        PurchaseItemDTO item = item(cartProductId);
        givenNoSuggestions();

        whenRequestingSuggestions(new CartSuggestionRequestDTO(
                studentId,
                buffetId,
                List.of(item),
                2
        ));

        GetCartSuggestionsRequest useCaseRequest = capturedRequest();
        assertEquals(studentId, useCaseRequest.studentId());
        assertEquals(buffetId, useCaseRequest.buffetId());
        assertEquals(java.util.Set.of(cartProductId), useCaseRequest.cartProductIds());
        assertEquals(2, useCaseRequest.limit());
    }

    @Test
    void suggestMapsApplicationResponseToWebResponse() {
        UUID suggestedProductId = randomUUID();
        givenSuggestions(suggestion(suggestedProductId));

        ResponseEntity<List<CartSuggestionResponseDTO>> response =
                whenRequestingSuggestions(new CartSuggestionRequestDTO(
                        randomUUID(),
                        randomUUID(),
                        List.of(),
                        2
                ));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(suggestedProductId, response.getBody().get(0).productId());
        assertEquals("Yogur", response.getBody().get(0).productName());
        assertEquals("FAVORITE", response.getBody().get(0).source());
    }

    @Test
    void suggestUsesEmptySetWhenRequestItemsAreNull() {
        UUID studentId = randomUUID();
        UUID buffetId = randomUUID();
        givenNoSuggestions();

        ResponseEntity<List<CartSuggestionResponseDTO>> response =
                whenRequestingSuggestions(new CartSuggestionRequestDTO(
                        studentId,
                        buffetId,
                        null,
                        null
                ));

        assertEquals(List.of(), response.getBody());

        GetCartSuggestionsRequest useCaseRequest = capturedRequest();
        assertEquals(studentId, useCaseRequest.studentId());
        assertEquals(buffetId, useCaseRequest.buffetId());
        assertEquals(java.util.Set.of(), useCaseRequest.cartProductIds());
        assertEquals(null, useCaseRequest.limit());
    }

    private void givenNoSuggestions() {
        givenSuggestions();
    }

    private void givenSuggestions(GetCartSuggestionsResponse.Suggestion... suggestions) {
        when(getCartSuggestions.execute(any()))
                .thenReturn(new GetCartSuggestionsResponse(List.of(suggestions)));
    }

    private ResponseEntity<List<CartSuggestionResponseDTO>> whenRequestingSuggestions(
            CartSuggestionRequestDTO request) {
        return controller.suggest(request);
    }

    private GetCartSuggestionsRequest capturedRequest() {
        ArgumentCaptor<GetCartSuggestionsRequest> captor =
                ArgumentCaptor.forClass(GetCartSuggestionsRequest.class);
        verify(getCartSuggestions).execute(captor.capture());
        return captor.getValue();
    }

    private PurchaseItemDTO item(UUID productId) {
        return new PurchaseItemDTO(
                productId,
                "Agua",
                1,
                new BigDecimal("100.00")
        );
    }

    private GetCartSuggestionsResponse.Suggestion suggestion(UUID productId) {
        return new GetCartSuggestionsResponse.Suggestion(
                productId,
                "Yogur",
                new BigDecimal("120.00"),
                4,
                "Motivo",
                "FAVORITE",
                50
        );
    }
}

