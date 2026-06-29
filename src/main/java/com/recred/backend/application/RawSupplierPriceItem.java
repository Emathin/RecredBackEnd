package com.recred.backend.application;

import java.math.BigDecimal;

public record RawSupplierPriceItem(
        String nombreProducto,
        BigDecimal precio,
        String unidad,
        String notas
) {}
