package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ItemListaPrecioProveedor {
    private UUID id;
    private UUID listaPrecioId;
    private String nombreProductoProveedor;
    private UUID productoInventarioId;
    private String nombreProductoInventario;
    private BigDecimal precio;
    private String unidad;
    private String notas;
    private boolean mappingConfirmado = false;
}
