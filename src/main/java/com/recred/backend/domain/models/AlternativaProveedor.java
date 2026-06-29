package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

public record AlternativaProveedor(
        UUID proveedorId,
        String nombreProveedor,
        BigDecimal precio,
        String unidad
) {}
