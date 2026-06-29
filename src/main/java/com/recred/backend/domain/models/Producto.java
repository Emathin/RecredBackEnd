package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Producto {

    private UUID id;
    private UUID promocionId;
    private String promocionNombre;
    private CategoriaProducto categoria;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private BigDecimal peso;
    private Boolean requierePreparacion;
    private Boolean activo = true;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
    private String urlImagen;

    private Integer stockActual;
    private Integer stockReservado;
    private Integer stockMinimo;
    private Integer stockDisponible;
    private EstadoInventario estadoInventario;
    private TipoManejoInventario tipoManejoInventario;
    private Integer cupoMaximoDiario;
    private Integer cupoDisponibleDia;

    private Set<ProductoMedida> medidas = new HashSet<>();
    private Set<ClasificacionTipo> tipos = new HashSet<>();
    private Set<ClasificacionSalud> clasificacionesSalud = new HashSet<>();

    public void addMedida(ProductoMedida medida) {
        if (medida != null) {
            this.medidas.add(medida);
            medida.setProducto(this);
        }
    }

    public void removeMedida(ProductoMedida medida) {
        if (medida != null) {
            this.medidas.remove(medida);
            medida.setProducto(null);
        }
    }

    public Set<ClasificacionSalud> getClasificaciones() {
        return clasificacionesSalud;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(activo);
    }

    public void deactivate() {
        this.activo = false;
    }

    public boolean hasInventoryData() {
        return stockActual != null
                || stockReservado != null
                || stockMinimo != null
                || cupoMaximoDiario != null
                || estadoInventario != null
                || tipoManejoInventario != null;
    }

    public boolean needsRestock() {
        return isSoldOut()
                || estadoInventario == EstadoInventario.BAJO_STOCK
                || hasLowStock();
    }

    public boolean hasLowStock() {
        return getStockDisponible() != null
                && stockMinimo != null
                && getStockDisponible() <= stockMinimo;
    }

    public boolean hasLowStockStatus() {
        return estadoInventario == EstadoInventario.BAJO_STOCK;
    }

    public boolean isSoldOut() {
        if (estadoInventario == EstadoInventario.SIN_STOCK
                || estadoInventario == EstadoInventario.DESACTIVADO) {
            return true;
        }

        return getStockDisponible() != null && getStockDisponible() <= 0;
    }

    public boolean isAvailableForSale() {
        return !isSoldOut();
    }

    public TipoManejoInventario inventoryMode() {
        return tipoManejoInventario == null
                ? TipoManejoInventario.STOCK_EXACTO
                : tipoManejoInventario;
    }

    public Integer configuredDailyCapacity() {
        if (inventoryMode() != TipoManejoInventario.CUPO_DIARIO) {
            return null;
        }

        return cupoMaximoDiario == null ? stockActual : cupoMaximoDiario;
    }

    public Integer dailyCapacityAvailable() {
        return inventoryMode() == TipoManejoInventario.CUPO_DIARIO
                ? getStockDisponible()
                : null;
    }

    public boolean hasDailyCapacityLimit() {
        return inventoryMode() == TipoManejoInventario.CUPO_DIARIO
                && cupoMaximoDiario != null
                && cupoMaximoDiario > 0;
    }

    public int reservedStockOrZero() {
        return stockReservado == null ? 0 : stockReservado;
    }

    public BigDecimal priceOrZero() {
        return precio == null ? BigDecimal.ZERO : precio;
    }

    public Integer getStockDisponible() {
        if (stockDisponible != null) {
            return stockDisponible;
        }

        if (stockActual == null) {
            return null;
        }

        int reserved = stockReservado == null ? 0 : stockReservado;
        return Math.max(0, stockActual - reserved);
    }
}
