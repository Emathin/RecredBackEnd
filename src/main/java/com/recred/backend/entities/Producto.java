package com.recred.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(precision = 6, scale = 2)
    private BigDecimal peso;

    // Tabla intermedia producto_tipo
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "producto_tipo",
            joinColumns = @JoinColumn(name = "producto_id"),
            inverseJoinColumns = @JoinColumn(name = "tipo_id")
    )
    private Set<ClasificacionTipo> tipos = new HashSet<>();

    // Tabla intermedia productos_clasificacion_salud
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "productos_clasificacion_salud",
            joinColumns = @JoinColumn(name = "producto_id"),
            inverseJoinColumns = @JoinColumn(name = "clasificacion_salud_id")
    )
    private Set<ClasificacionSalud> clasificacionesSalud = new HashSet<>();
}