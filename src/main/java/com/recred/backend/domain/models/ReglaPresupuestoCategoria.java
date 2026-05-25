package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

public class ReglaPresupuestoCategoria {
    private UUID id;
    private CategoriaProducto categoria;
    private BigDecimal montoLimite;

    public ReglaPresupuestoCategoria() {}

    public ReglaPresupuestoCategoria(UUID id, CategoriaProducto categoria, BigDecimal montoLimite) {
        this.id = id;
        this.categoria = categoria;
        this.montoLimite = montoLimite;
    }

    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }
    public CategoriaProducto getCategoria() {
        return categoria;
    }
    public void setCategoria(CategoriaProducto categoria) {
        this.categoria = categoria;
    }
    public BigDecimal getMontoLimite() {
        return montoLimite;
    }
    public void setMontoLimite(BigDecimal montoLimite) {
        this.montoLimite = montoLimite;
    }
}