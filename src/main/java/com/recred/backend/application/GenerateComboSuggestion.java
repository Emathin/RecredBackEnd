package com.recred.backend.application;

import com.recred.backend.domain.models.ComboSuggestion;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

public class GenerateComboSuggestion {

    private final ProductRepository productRepository;
    private final KiosqueroRepository kiosqueroRepository;

    public GenerateComboSuggestion(ProductRepository productRepository,
                                   KiosqueroRepository kiosqueroRepository) {
        this.productRepository = productRepository;
        this.kiosqueroRepository = kiosqueroRepository;
    }

    @Transactional(readOnly = true)
    public ComboSuggestion execute(UUID productId, UUID userId) {
        UUID buffetId = kiosqueroRepository.findBuffetByUsuarioId(userId);
        if (buffetId == null) {
            throw new RuntimeException("Buffet not found for user");
        }

        var product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        List<Producto> topSoldProducts = productRepository.findBuffetTop3MostSold(
                buffetId, product.getCategoria().getId());

        List<ComboSuggestion.SuggestedProduct> suggestions = topSoldProducts.stream()
                .map(p -> new ComboSuggestion.SuggestedProduct(
                        p.getId(), p.getNombre(), p.getPrecio()))
                .collect(Collectors.toList());

        return new ComboSuggestion(productId, product.getNombre(), suggestions);
    }
}
