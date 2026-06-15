package com.recred.backend.domain.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComboSuggestion {

    private UUID idProduct;
    private String productName;
    private List<SuggestedProduct> suggestedProducts;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SuggestedProduct {
        private UUID id;
        private String nombre;
        private BigDecimal precio;
    }
}
