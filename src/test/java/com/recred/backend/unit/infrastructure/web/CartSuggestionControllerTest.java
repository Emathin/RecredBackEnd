package com.recred.backend.unit.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.GetCartSuggestions;
import com.recred.backend.application.GetCartSuggestionsRequest;
import com.recred.backend.application.GetCartSuggestionsResponse;
import com.recred.backend.infrastructure.web.CartSuggestionController;
import com.recred.backend.infrastructure.web.CartSuggestionRequestDTO;
import com.recred.backend.infrastructure.web.PurchaseItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CartSuggestionControllerTest {

    private static final String CART_SUGGESTIONS_URL = "/api/v1/cart-suggestions";
    private static final UUID STUDENT_ID = randomUUID();
    private static final UUID BUFFET_ID = randomUUID();
    private static final UUID CART_PRODUCT_ID = randomUUID();
    private static final UUID SUGGESTED_PRODUCT_ID = randomUUID();

    @Mock
    private GetCartSuggestions getCartSuggestions;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        CartSuggestionController controller = new CartSuggestionController(getCartSuggestions);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void suggestMapsRequestItemsToUseCaseRequest() throws Exception {
        givenNoSuggestions();

        whenRequestingSuggestionsWithItems();

        thenRequestIsMappedToUseCaseWithItems();
    }

    @Test
    void suggestMapsApplicationResponseToWebResponse() throws Exception {
        givenSuggestions(suggestion(SUGGESTED_PRODUCT_ID));

        ResultActions result = whenRequestingSuggestions();

        thenSuggestionsAreReturned(result);
    }

    @Test
    void suggestUsesEmptySetWhenRequestItemsAreNull() throws Exception {
        givenNoSuggestions();

        ResultActions result = whenRequestingSuggestionsWithoutItems();

        thenEmptySuggestionsResponseIsReturned(result);
        thenRequestIsMappedToUseCaseWithoutItems();
    }

    private void givenNoSuggestions() {
        givenSuggestions();
    }

    private void givenSuggestions(GetCartSuggestionsResponse.Suggestion... suggestions) {
        when(getCartSuggestions.execute(any(GetCartSuggestionsRequest.class)))
                .thenReturn(new GetCartSuggestionsResponse(List.of(suggestions)));
    }

    private ResultActions whenRequestingSuggestionsWithItems() throws Exception {
        CartSuggestionRequestDTO request = new CartSuggestionRequestDTO(
                STUDENT_ID,
                BUFFET_ID,
                List.of(item(CART_PRODUCT_ID)),
                2
        );
        return mockMvc.perform(post(CART_SUGGESTIONS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions whenRequestingSuggestions() throws Exception {
        CartSuggestionRequestDTO request = new CartSuggestionRequestDTO(
                STUDENT_ID,
                BUFFET_ID,
                List.of(),
                2
        );
        return mockMvc.perform(post(CART_SUGGESTIONS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions whenRequestingSuggestionsWithoutItems() throws Exception {
        CartSuggestionRequestDTO request = new CartSuggestionRequestDTO(
                STUDENT_ID,
                BUFFET_ID,
                null,
                null
        );
        return mockMvc.perform(post(CART_SUGGESTIONS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private void thenRequestIsMappedToUseCaseWithItems() {
        GetCartSuggestionsRequest useCaseRequest = capturedRequest();
        assertEquals(STUDENT_ID, useCaseRequest.studentId());
        assertEquals(BUFFET_ID, useCaseRequest.buffetId());
        assertEquals(java.util.Set.of(CART_PRODUCT_ID), useCaseRequest.cartProductIds());
        assertEquals(2, useCaseRequest.limit());
    }

    private void thenRequestIsMappedToUseCaseWithoutItems() {
        GetCartSuggestionsRequest useCaseRequest = capturedRequest();
        assertEquals(STUDENT_ID, useCaseRequest.studentId());
        assertEquals(BUFFET_ID, useCaseRequest.buffetId());
        assertEquals(java.util.Set.of(), useCaseRequest.cartProductIds());
        assertEquals(null, useCaseRequest.limit());
    }

    private void thenSuggestionsAreReturned(ResultActions result) throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(SUGGESTED_PRODUCT_ID.toString()))
                .andExpect(jsonPath("$[0].productName").value("Yogur"))
                .andExpect(jsonPath("$[0].source").value("FAVORITE"));
    }

    private void thenEmptySuggestionsResponseIsReturned(ResultActions result) throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
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
