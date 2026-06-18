package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
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
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
public class PromotionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "promotions_products",
        joinColumns = @JoinColumn(name = "promotion_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private Set<ProductoEntity> products = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buffet_id")
    private BuffetEntity buffetEntity;

    public Promotion toDomain() {
        Promotion domain = new Promotion();
        domain.setId(this.id);
        domain.setName(this.name);
        domain.setDiscountPercentage(this.discountPercentage);
        domain.setStartDate(this.startDate);
        domain.setEndDate(this.endDate);
        domain.setStatus(this.status);
        domain.setCreatedAt(this.createdAt);
        domain.setUpdatedAt(this.updatedAt);
        
        if (this.products != null && org.hibernate.Hibernate.isInitialized(this.products)) {
            domain.setProducts(this.products.stream()
                    .map(ProductoEntity::toDomain)
                    .collect(Collectors.toList()));
        }
        
        if (this.buffetEntity != null) {
            domain.setBuffet(this.buffetEntity.toDomain());
        }
        
        return domain;
    }
}
