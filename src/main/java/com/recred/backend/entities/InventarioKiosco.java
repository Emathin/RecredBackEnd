package com.recred.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "inventarios_kiosco")
@Getter
@Setter
@NoArgsConstructor
public class InventarioKiosco {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kiosco_id", nullable = false)
    private Kiosco kiosco;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "stock", nullable = false)
    private Integer stock = 0;

    @Column(name = "stock_mostrador")
    private Integer stockMostrador = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoInventario estado = EstadoInventario.DISPONIBLE;
}