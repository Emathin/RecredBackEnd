package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    public boolean hasAvailableStock() {
        return getStockDisponible() != null && getStockDisponible() > 0;
    }

    public boolean hasHealthClassification(ClasificacionSalud classification) {
        if (classification == null || classification.getId() == null
                || getClasificaciones() == null) {
            return false;
        }

        return getClasificaciones().stream()
                .anyMatch(current -> Objects.equals(current.getId(), classification.getId()));
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

    public void applyDailyCapacity(long reserved) {
        if (!hasDailyCapacityLimit()) {
            return;
        }

        this.cupoDisponibleDia = Math.max(0, cupoMaximoDiario - (int) reserved);
    }

    public boolean isDailyCapacityExhausted() {
        return hasDailyCapacityLimit()
                && cupoDisponibleDia != null
                && cupoDisponibleDia <= 0;
    }

    public boolean exceedsPrice(BigDecimal availableAmount) {
        return availableAmount != null && priceOrZero().compareTo(availableAmount) > 0;
    }

    public boolean exceedsCategoryBudget(Map<UUID, BigDecimal> availableByCategory) {
        if (categoria == null || availableByCategory == null) {
            return false;
        }

        return exceedsPrice(availableByCategory.get(categoria.getId()));
    }

    public boolean matchesAnyCategoryDescription(List<String> descriptions) {
        if (descriptions == null || descriptions.isEmpty()) {
            return false;
        }

        return descriptions.stream().anyMatch(this::matchesCategoryDescription);
    }

    public boolean matchesCategoryDescription(String description) {
        return categoria != null
                && categoria.getDescripcion() != null
                && description != null
                && categoria.getDescripcion().toLowerCase().contains(description.toLowerCase());
    }

    public String idAsString() {
        return id == null ? null : id.toString();
    }

    public boolean hasSamePurchaseIdentityAs(Producto other) {
        return other != null
                && Objects.equals(id, other.getId())
                && priceOrZero().compareTo(other.priceOrZero()) == 0
                && Objects.equals(promocionId, other.getPromocionId());
    }

    public Producto discountedFor(Promotion promotion) {
        Producto discountedProduct = copyForPurchase();
        BigDecimal discountedPrice = priceOrZero()
                .multiply(promotion.discountMultiplier())
                .setScale(2, RoundingMode.HALF_UP);
        discountedProduct.setPrecio(discountedPrice);
        discountedProduct.setPromocionId(promotion.getId());
        discountedProduct.setPromocionNombre(promotion.getName());
        return discountedProduct;
    }

    public Producto copyForPurchase() {
        Producto product = new Producto();
        product.setId(id);
        product.setCategoria(categoria);
        product.setNombre(nombre);
        product.setDescripcion(descripcion);
        product.setPeso(peso);
        product.setRequierePreparacion(requierePreparacion);
        product.setActivo(activo);
        product.setCreadoEn(creadoEn);
        product.setActualizadoEn(actualizadoEn);
        product.setUrlImagen(urlImagen);
        product.setStockActual(stockActual);
        product.setStockReservado(stockReservado);
        product.setStockMinimo(stockMinimo);
        product.setStockDisponible(stockDisponible);
        product.setEstadoInventario(estadoInventario);
        product.setTipoManejoInventario(tipoManejoInventario);
        product.setCupoMaximoDiario(cupoMaximoDiario);
        product.setCupoDisponibleDia(cupoDisponibleDia);
        product.setMedidas(medidas);
        product.setTipos(tipos);
        product.setClasificacionesSalud(clasificacionesSalud);
        return product;
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
