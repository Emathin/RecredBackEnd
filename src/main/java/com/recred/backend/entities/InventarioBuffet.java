package com.recred.backend.entities;

import jakarta.persistence.*;
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
public class InventarioBuffet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buffet_id", nullable = false)
    private Buffet buffet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual = 0;

    @Column(name = "stock_minimo")
    private Integer stockMinimo = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoInventario estado = EstadoInventario.DISPONIBLE;

    @OneToMany(mappedBy = "inventario", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<MovimientoStock> movimientos = new java.util.ArrayList<>();

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;
}