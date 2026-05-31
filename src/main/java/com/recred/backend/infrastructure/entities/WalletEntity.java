package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.Wallet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "billeteras")
@Getter
@Setter
@NoArgsConstructor
public class WalletEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false, unique = true)
    private StudentEntity studentEntity;

    @Column(name = "saldo_creditos", nullable = false, precision = 12, scale = 2)
    private BigDecimal creditBalance = BigDecimal.ZERO;

    @Column(name = "limite_credito", precision = 12, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "saldo_deuda_interno", precision = 12, scale = 2)
    private BigDecimal innerDebtBalance = BigDecimal.ZERO;

    @Column(name = "credito_habilitado", nullable = false)
    private Boolean enabledCredit = false;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime updatedAt;

    public Wallet toDomain() {
        Wallet domain = new Wallet();
        domain.setId(this.id);
        domain.setCreditBalance(this.creditBalance);
        domain.setCreditLimit(this.creditLimit);
        domain.setInnerDebtBalance(this.innerDebtBalance);
        domain.setEnabledCredit(this.enabledCredit);
        domain.setCreatedAt(this.createdAt);
        domain.setUpdatedAt(this.updatedAt);
        return domain;
    }

    public static WalletEntity fromDomain(Wallet wallet) {
        if (wallet == null) {
            return null;
        }

        WalletEntity entity = new WalletEntity();

        entity.setId(wallet.getId());
        entity.setCreditBalance(wallet.getCreditBalance());
        entity.setCreditLimit(wallet.getCreditLimit());
        entity.setInnerDebtBalance(wallet.getInnerDebtBalance());
        entity.setEnabledCredit(wallet.getEnabledCredit());
        entity.setCreatedAt(wallet.getCreatedAt());
        entity.setUpdatedAt(wallet.getUpdatedAt());

        return entity;
    }
}
