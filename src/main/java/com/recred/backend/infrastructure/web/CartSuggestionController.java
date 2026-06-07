package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetCartSuggestions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/cart-suggestions")
public class CartSuggestionController {

    private final GetCartSuggestions getCartSuggestions;

    public CartSuggestionController(GetCartSuggestions getCartSuggestions) {
        this.getCartSuggestions = getCartSuggestions;
    }

    @PostMapping
    public ResponseEntity<List<CartSuggestionResponse>> suggest(
            @RequestBody CartSuggestionRequest request) {
        Set<UUID> cartProductIds = request.items() == null
                ? Set.of()
                : request.items().stream()
                        .map(PurchaseItemDTO::getProductId)
                        .collect(Collectors.toSet());

        List<CartSuggestionResponse> suggestions = getCartSuggestions.execute(
                        request.studentId(),
                        request.buffetId(),
                        cartProductIds,
                        request.limit()
                )
                .stream()
                .map(CartSuggestionResponse::fromDomain)
                .toList();

        return ResponseEntity.ok(suggestions);
    }
}
