package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetCartSuggestions;
import com.recred.backend.application.GetCartSuggestionsRequest;
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
    private final SchoolLicenseAccessGuard schoolLicenseAccessGuard;

    public CartSuggestionController(
            GetCartSuggestions getCartSuggestions,
            SchoolLicenseAccessGuard schoolLicenseAccessGuard
    ) {
        this.getCartSuggestions = getCartSuggestions;
        this.schoolLicenseAccessGuard = schoolLicenseAccessGuard;
    }

    @PostMapping
    public ResponseEntity<List<CartSuggestionResponseDTO>> suggest(
            @RequestBody CartSuggestionRequestDTO request) {
        schoolLicenseAccessGuard.requireActiveSchoolForStudent(request.studentId());
        List<CartSuggestionResponseDTO> suggestions = getCartSuggestions
                .execute(toUseCaseRequest(request))
                .suggestions()
                .stream()
                .map(CartSuggestionResponseDTO::fromApplication)
                .toList();

        return ResponseEntity.ok(suggestions);
    }

    private GetCartSuggestionsRequest toUseCaseRequest(CartSuggestionRequestDTO request) {
        return new GetCartSuggestionsRequest(
                request.studentId(),
                request.buffetId(),
                cartProductIds(request),
                request.limit());
    }

    private Set<UUID> cartProductIds(CartSuggestionRequestDTO request) {
        if (request.items() == null) {
            return Set.of();
        }
        return request.items().stream()
                .map(PurchaseItemDTO::getProductId)
                .collect(Collectors.toSet());
    }
}
