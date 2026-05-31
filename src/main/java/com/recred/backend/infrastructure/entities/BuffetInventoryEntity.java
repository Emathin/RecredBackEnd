package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.InventoryState;
import com.recred.backend.domain.models.Inventory;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventarios_buffet")
@Getter
@Setter
@NoArgsConstructor
public class BuffetInventoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buffet_id", nullable = false)
    private BuffetEntity buffetEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private ProductEntity productEntity;

    @Column(name = "stock_actual", nullable = false)
    private Integer currentStock = 0;

    @Column(name = "stock_minimo")
    private Integer minimumStock = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryState state = InventoryState.DISPONIBLE;

    @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<StockMovementEntity> movements = new java.util.ArrayList<>();

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime updatedAt;

    public Inventory toDomain() {
        return new Inventory(
                this.id,
                this.buffetEntity != null ? this.buffetEntity.getId() : null,
                this.productEntity != null ? this.productEntity.getId() : null,
                this.currentStock,
                this.minimumStock,
                this.state,
                this.updatedAt
        );
    }
    }