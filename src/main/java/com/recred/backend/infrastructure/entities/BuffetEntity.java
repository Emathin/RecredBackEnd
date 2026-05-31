package com.recred.backend.infrastructure.entities;


import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Merchant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "buffets")
@Getter
@Setter
@NoArgsConstructor
public class BuffetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colegio_id", nullable = false)
    private SchoolEntity schoolEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private MerchantEntity merchant;

    @Column(nullable = false)
    private String name;

    @Column(name = "fecha_vencimiento_habilitacion")
    private LocalDate contractExpirationDate;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime updatedAt;

    public Buffet toDomain() {
        Buffet buffet = new Buffet();

        buffet.setId(this.id);
        buffet.setName(this.name);
        buffet.setContractExpirationDate(this.contractExpirationDate);
        buffet.setActive(this.active);
        buffet.setCreatedAt(this.createdAt);
        buffet.setUpdatedAt(this.updatedAt);

        if (this.schoolEntity != null) {
            buffet.setSchool(this.schoolEntity.toDomain());
        }

        if (this.merchant != null) {

            Merchant merchantDominio = new Merchant();
            merchantDominio.setId(this.merchant.getId());
            merchantDominio.setCuit(this.merchant.getCuit());

            buffet.setMerchant(merchantDominio);
        }

        return buffet;
    }
}