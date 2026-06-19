package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetCartSuggestions;
import com.recred.backend.domain.models.CartSuggestion;
import com.recred.backend.domain.models.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @SuppressWarnings("unchecked")
    void suggestMapsRequestItemsAndResponse() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        UUID cartProductId = UUID.randomUUID();
        UUID suggestedProductId = UUID.randomUUID();
        PurchaseItemDTO item = new PurchaseItemDTO(
                cartProductId,
                "Agua",
                1,
                new BigDecimal("100.00")
        );
        CartSuggestion suggestion = new CartSuggestion(
                product(suggestedProductId),
                "Motivo",
                "FAVORITE",
                50
        );
        when(getCartSuggestions.execute(
                org.mockito.ArgumentMatchers.eq(studentId),
                org.mockito.ArgumentMatchers.eq(buffetId),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(2)
        )).thenReturn(List.of(suggestion));

        ResponseEntity<List<CartSuggestionResponse>> response = controller.suggest(
                new CartSuggestionRequest(studentId, buffetId, List.of(item), 2)
        );

        assertEquals(200, response.getStatusCode().value());
        assertEquals(suggestedProductId, response.getBody().get(0).productId());
        assertEquals("Yogur", response.getBody().get(0).productName());
        assertEquals("FAVORITE", response.getBody().get(0).source());

        ArgumentCaptor<Set<UUID>> cartProductsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(getCartSuggestions).execute(
                org.mockito.ArgumentMatchers.eq(studentId),
                org.mockito.ArgumentMatchers.eq(buffetId),
                cartProductsCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(2)
        );
        assertEquals(Set.of(cartProductId), cartProductsCaptor.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void suggestUsesEmptySetWhenRequestItemsAreNull() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        when(getCartSuggestions.execute(
                org.mockito.ArgumentMatchers.eq(studentId),
                org.mockito.ArgumentMatchers.eq(buffetId),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(null)
        )).thenReturn(List.of());

        ResponseEntity<List<CartSuggestionResponse>> response = controller.suggest(
                new CartSuggestionRequest(studentId, buffetId, null, null)
        );

        assertEquals(List.of(), response.getBody());
        ArgumentCaptor<Set<UUID>> cartProductsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(getCartSuggestions).execute(
                org.mockito.ArgumentMatchers.eq(studentId),
                org.mockito.ArgumentMatchers.eq(buffetId),
                cartProductsCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(null)
        );
        assertEquals(Set.of(), cartProductsCaptor.getValue());
    }

    private Producto product(UUID productId) {
        Producto product = new Producto();
        product.setId(productId);
        product.setNombre("Yogur");
        product.setPrecio(new BigDecimal("120.00"));
        product.setStockActual(5);
        product.setStockReservado(1);
        return product;
    }
}
