package com.recred.backend.domain.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecomendacionProveedor {
    private UUID productoInventarioId;
    private String nombreProducto;
    private UUID proveedorRecomendadoId;
    private String nombreProveedorRecomendado;
    private BigDecimal mejorPrecio;
    private String unidad;
    private List<AlternativaProveedor> alternativas;
}
