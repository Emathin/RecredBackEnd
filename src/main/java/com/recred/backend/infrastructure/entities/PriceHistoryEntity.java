package com.recred.backend.infrastructure.entities;


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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "historial_precios")
@Getter
@Setter
@NoArgsConstructor
public class PriceHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private ProductEntity productEntity;

    @Column(name = "precio_anterior", nullable = false, precision = 10, scale = 2)
    private BigDecimal lastPrice;

    @Column(name = "precio_nuevo", nullable = false, precision = 10, scale = 2)
    private BigDecimal newPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cambiado_por_usuario_id")
    private UserEntity changedByUserEntity;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime changeDate = LocalDateTime.now();
}