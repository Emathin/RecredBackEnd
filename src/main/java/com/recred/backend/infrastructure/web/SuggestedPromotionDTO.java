package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public class SuggestedPromotionDTO {
    
    @JsonProperty("nombre")
    private String name;
    
    @JsonProperty("descuento")
    private BigDecimal discountPercentage;
    
    @JsonProperty("categorias_destino")
    private List<String> targetCategories;

    public SuggestedPromotionDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(BigDecimal discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public List<String> getTargetCategories() {
        return targetCategories;
    }

    public void setTargetCategories(List<String> targetCategories) {
        this.targetCategories = targetCategories;
    }
}
