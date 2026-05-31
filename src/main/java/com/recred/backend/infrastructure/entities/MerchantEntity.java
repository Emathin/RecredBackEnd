package com.recred.backend.infrastructure.entities;


import com.recred.backend.domain.models.Merchant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "kiosqueros")
@Getter
@Setter
@NoArgsConstructor
public class MerchantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(nullable = false, length = 20)
    private String cuit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buffet_id", nullable = false)
    private BuffetEntity buffetEntity;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime updatedAt;

    public static MerchantEntity fromDomain(Merchant merchant) {
        if (merchant == null) {
            return null;
        }

        MerchantEntity entity = new MerchantEntity();

        entity.setId(merchant.getId());
        entity.setCuit(merchant.getCuit());
        entity.setCreatedAt(merchant.getCreatedAt());
        entity.setUpdatedAt(merchant.getUpdatedAt());

        if (merchant.getBuffet() != null && merchant.getBuffet().getId() != null) {
            BuffetEntity buffetProxy = new BuffetEntity();
            buffetProxy.setId(merchant.getBuffet().getId());
            entity.setBuffetEntity(buffetProxy);
        }

        return entity;
    }

    public Merchant toDomain() {
        Merchant merchant = new Merchant();
        merchant.setId(this.getId());

        if (this.getBuffetEntity() != null) {
            merchant.setBuffet(this.getBuffetEntity().toDomain());
        }
        return merchant;
    }

}