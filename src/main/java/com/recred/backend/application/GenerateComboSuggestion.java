package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public class GenerateComboSuggestion {

    private final ProductRepository productRepository;
    private final KiosqueroRepository kiosqueroRepository;
    private final UsuarioRepository usuarioRepository;

    public GenerateComboSuggestion(ProductRepository productRepository,
                                   KiosqueroRepository kiosqueroRepository,
                                   UsuarioRepository usuarioRepository) {
        this.productRepository = productRepository;
        this.kiosqueroRepository = kiosqueroRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public GenerateComboSuggestionResponse execute(
            GenerateComboSuggestionRequest comboSuggestionRequest) {
        UUID buffetId = getBuffetIdForUser(comboSuggestionRequest.cognitoSubject());
        Producto product = getProduct(comboSuggestionRequest.productId());
        List<Producto> suggestedProducts = getTopSoldProducts(
                buffetId,
                product.getCategoria().getId());

        return buildResponse(product, suggestedProducts);
    }

    private UUID getBuffetIdForUser(String cognitoSubject) {
        Usuario user = getUser(cognitoSubject);
        UUID buffetId = kiosqueroRepository.findBuffetByUsuarioId(user.getId());
        if (buffetId == null) {
            throw new RuntimeException("Buffet not found for user");
        }
        return buffetId;
    }

    private Usuario getUser(String cognitoSubject) {
        return usuarioRepository.findByIdCognito(cognitoSubject)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Producto getProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    private List<Producto> getTopSoldProducts(UUID buffetId, UUID categoryId) {
        return productRepository.findBuffetTop3MostSold(buffetId, categoryId);
    }

    private GenerateComboSuggestionResponse buildResponse(
            Producto product, List<Producto> topSold) {
        List<GenerateComboSuggestionResponse
                .SuggestedProductResponse> suggestions = topSold.stream()
                .map(this::mapToSuggestedProductResponse)
                .toList();
        return new GenerateComboSuggestionResponse(
                product.getId(), product.getNombre(), suggestions);
    }

    private GenerateComboSuggestionResponse.SuggestedProductResponse mapToSuggestedProductResponse(
            Producto product) {
        return new GenerateComboSuggestionResponse.SuggestedProductResponse(
                product.getId(), product.getNombre(), product.getPrecio());
    }
}
