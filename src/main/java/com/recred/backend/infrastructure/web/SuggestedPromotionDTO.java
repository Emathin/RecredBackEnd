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
    
    @JsonProperty("imagen")
    private String imagen;

    @JsonProperty("productIds")
    private List<String> productIds;

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

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public List<String> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<String> productIds) {
        this.productIds = productIds;
    }
}
