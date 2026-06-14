package com.recred.backend.domain.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ProductSaleStatistics {
    private UUID productoId;
    private String nombre;
    private String categoria;
    private BigDecimal precioActual;
    private Long ventasPeriodo;
    private BigDecimal participacionVentas;

    private Integer rankingGeneral;
    private Integer rankingCategoria;

    private BigDecimal promedioVentasCategoria;
    private BigDecimal promedioPrecioCategoria;

    private BigDecimal diferenciaPrecioCategoria;

    private Long diasSinVenta;

    private Long clientesDistintos;

    private Integer stockActual;
    private BigDecimal stockPromedioCategoria;
}
