package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Product {

    private UUID id;
    private ProductCategory category;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal weight;
    private Boolean requiresPreparation;
    private Boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Integer currentStock;

    private Set<ProductMeasure> measures = new HashSet<>();
    private Set<TypeClassification> types = new HashSet<>();
    private Set<HealthClassification> healthClassifications = new HashSet<>();

    public void addMeasure(ProductMeasure measure) {
        if (measure != null) {
            this.measures.add(measure);
            measure.setProduct(this);
        }
    }

    public void removeMeasure(ProductMeasure measure) {
        if (measure != null) {
            this.measures.remove(measure);
            measure.setProduct(null);
        }
    }

}