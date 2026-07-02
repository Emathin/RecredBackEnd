package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;
import java.text.Normalizer;
import java.util.List;
import java.util.Optional;

class SupplierProductAutoMatcher {

    Optional<Producto> match(String supplierProductName, List<Producto> products) {
        if (supplierProductName == null || supplierProductName.isBlank()) {
            return Optional.empty();
        }

        String normalizedSupplierProduct = normalize(supplierProductName);
        Optional<Producto> exactMatch = exactMatch(normalizedSupplierProduct, products);
        return exactMatch.isPresent()
                ? exactMatch
                : partialMatch(normalizedSupplierProduct, products);
    }

    private Optional<Producto> exactMatch(
            String normalizedSupplierProduct,
            List<Producto> products
    ) {
        return products.stream()
                .filter(product -> normalize(product.getNombre())
                        .equals(normalizedSupplierProduct))
                .findFirst();
    }

    private Optional<Producto> partialMatch(
            String normalizedSupplierProduct,
            List<Producto> products
    ) {
        return products.stream()
                .filter(product -> hasPartialMatch(
                        normalizedSupplierProduct,
                        normalize(product.getNombre())
                ))
                .findFirst();
    }

    private boolean hasPartialMatch(String first, String second) {
        return !first.isBlank()
                && !second.isBlank()
                && (first.contains(second) || second.contains(first));
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
}
