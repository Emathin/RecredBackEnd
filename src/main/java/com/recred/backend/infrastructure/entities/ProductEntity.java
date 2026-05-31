package com.recred.backend.infrastructure.entities;


import com.recred.backend.domain.models.HealthClassification;
import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private ProductCategoryEntity category;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 6, scale = 2)
    private BigDecimal weight;

    @Column(name = "requiere_preparacion", nullable = false)
    private Boolean requiresPreparation;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductMeasureEntity> measures = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "productos_clasificaciones",
            joinColumns = @JoinColumn(name = "producto_id"),
            inverseJoinColumns = @JoinColumn(name = "clasificacion_id")
    )
    private Set<TypeClassificationEntity> types = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "productos_clasificacion_salud",
            joinColumns = @JoinColumn(name = "producto_id"),
            inverseJoinColumns = @JoinColumn(name = "clasificacion_salud_id")
    )
    private Set<HealthClassificationEntity> healthClassifications = new HashSet<>();

    public void addMedida(ProductMeasureEntity medida) {
        measures.add(medida);
        medida.setProduct(this);
    }

    public void removeMedida(ProductMeasureEntity medida) {
        measures.remove(medida);
        medida.setProduct(null);
    }

    public Product toDomain() {
        Product domain = new Product();
        domain.setId(this.id);
        domain.setName(this.name);
        domain.setDescription(this.description);
        domain.setPrice(this.price);
        domain.setWeight(this.weight);
        domain.setRequiresPreparation(this.requiresPreparation);
        domain.setActive(this.active);
        domain.setCreatedAt(this.createdAt);
        domain.setUpdatedAt(this.updatedAt);

        if (this.healthClassifications != null) {
            domain.setHealthClassifications(this.healthClassifications.stream().map(c -> {
                HealthClassification cs = new HealthClassification();
                cs.setId(c.getId());
                cs.setDescription(c.getDescription());
                cs.setActive(c.getActive());
                return cs;
            }).collect(Collectors.toSet()));
        }

        return domain;
    }

    public ProductChangeRecommendation toProductRecommendation(UUID buffetId) {
        ProductChangeRecommendation producto= new ProductChangeRecommendation();
        producto.setIdBuffet(buffetId);
        producto.setId(this.id);
        producto.setName(this.name);
        producto.setPrice(this.price);
        return producto;

    }
}