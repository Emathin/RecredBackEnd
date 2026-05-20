package com.recred.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "productos_medidas")
@Getter
@Setter
@NoArgsConstructor
public class ProductoMedida {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "cantidad_unidad", nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidadUnidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnidadMedida unidad;
}