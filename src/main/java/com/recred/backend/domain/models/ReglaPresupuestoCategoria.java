package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

public class ReglaPresupuestoCategoria {
    private UUID id;
    private CategoriaProducto categoria;
    private BigDecimal porcentajeLimite;
    private BigDecimal montoLimiteCalculado;
    private boolean activo;

    public ReglaPresupuestoCategoria() {
        this.activo = true;
    }

    public ReglaPresupuestoCategoria(UUID id, CategoriaProducto categoria,
                                     BigDecimal porcentajeLimite, BigDecimal montoLimiteCalculado,
                                     boolean activo) {
        this.id = id;
        this.categoria = categoria;
        this.porcentajeLimite = porcentajeLimite;
        this.montoLimiteCalculado = montoLimiteCalculado;
        this.activo = activo;
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

    public BigDecimal getPorcentajeLimite() {
        return porcentajeLimite;
    }
    public void setPorcentajeLimite(BigDecimal
                                            porcentajeLimite) {
        this.porcentajeLimite = porcentajeLimite;
    }

    public BigDecimal getMontoLimiteCalculado() {
        return montoLimiteCalculado;
    }
    public void setMontoLimiteCalculado(BigDecimal
                                                montoLimiteCalculado) {
        this.montoLimiteCalculado = montoLimiteCalculado;
    }

    public boolean isActivo() {
        return activo;
    }
    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}