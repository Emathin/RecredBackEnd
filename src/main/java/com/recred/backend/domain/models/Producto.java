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
