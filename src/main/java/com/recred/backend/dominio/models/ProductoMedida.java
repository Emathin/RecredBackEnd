package com.recred.backend.dominio.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ProductoMedida {

    private UUID id;
    private Producto producto;
    private BigDecimal cantidadUnidad;
    private UnidadMedida unidad;
}