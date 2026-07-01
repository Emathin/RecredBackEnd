package com.recred.backend.domain.models;

import com.recred.backend.domain.exceptions.InsufficientStockException;
import com.recred.backend.domain.exceptions.InventoryOperationException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Inventario {
    public static final int STOCK_MINIMO_PREDETERMINADO = 4;

    private UUID id;
    private UUID buffetId;
    private UUID productoId;
    private Integer stockActual;
    private Integer stockReservado;
    private Integer stockMinimo;
    private EstadoInventario estado;
    private TipoManejoInventario tipoManejo;
    private Integer cupoMaximoDiario;
    private String urlImagen;
    private LocalDateTime actualizadoEn;

    public int stockDisponible() {
        if (!requiresQuantityControl()) {
            return isAvailableWithoutQuantityControl() ? 1 : 0;
        }

        return Math.max(0, currentStockOrZero() - reservedStockOrZero());
    }

    public void reservar(int cantidad) {
        validarCantidad(cantidad);

        if (!requiresQuantityControl()) {
            validarDisponibleSinControlDeCantidad();
            return;
        }

        validarDisponibilidad(cantidad);
        stockReservado = reservedStockOrZero() + cantidad;
        actualizarEstado();
    }

    public void consumir(int cantidad) {
        validarCantidad(cantidad);

        if (!requiresQuantityControl()) {
            validarDisponibleSinControlDeCantidad();
            return;
        }

        validarDisponibilidad(cantidad);
        stockActual = currentStockOrZero() - cantidad;
        actualizarEstado();
    }

    public void consumirReserva(int cantidad) {
        validarCantidad(cantidad);

        if (!requiresQuantityControl()) {
            validarDisponibleSinControlDeCantidad();
            return;
        }

        validarReserva(cantidad);
        stockActual = currentStockOrZero() - cantidad;
        stockReservado = reservedStockOrZero() - cantidad;
        actualizarEstado();
    }

    public void liberarReserva(int cantidad) {
        validarCantidad(cantidad);

        if (!requiresQuantityControl()) {
            return;
        }

        validarReserva(cantidad);
        stockReservado = reservedStockOrZero() - cantidad;
        actualizarEstado();
    }

    public void ajustarStock(int nuevoStock) {
        if (nuevoStock < 0) {
            throw new InventoryOperationException("El stock no puede ser negativo.");
        }

        if (requiresQuantityControl() && nuevoStock < reservedStockOrZero()) {
            throw new InventoryOperationException(
                    "El stock ajustado no puede ser menor al stock reservado.");
        }

        stockActual = nuevoStock;
        actualizarEstado();
    }

    public EstadoInventario estadoActual() {
        return estadoSeguro() == EstadoInventario.DESACTIVADO
                ? EstadoInventario.DESACTIVADO
                : estadoConInventarioActivo();
    }

    private EstadoInventario estadoConInventarioActivo() {
        return requiresQuantityControl()
                ? estadoConControlDeCantidad()
                : estadoSeguro();
    }

    private EstadoInventario estadoConControlDeCantidad() {
        return stockDisponible() <= 0
                ? EstadoInventario.SIN_STOCK
                : estadoConStockDisponible();
    }

    private EstadoInventario estadoConStockDisponible() {
        return esBajoStock()
                ? EstadoInventario.BAJO_STOCK
                : EstadoInventario.DISPONIBLE;
    }

    public boolean requiresQuantityControl() {
        return inventoryMode() != TipoManejoInventario.DISPONIBLE_NO_DISPONIBLE;
    }

    public boolean isAvailableWithoutQuantityControl() {
        return estadoSeguro() != EstadoInventario.SIN_STOCK
                && estadoSeguro() != EstadoInventario.DESACTIVADO;
    }

    public int currentStockOrZero() {
        return stockActual == null ? 0 : stockActual;
    }

    public int reservedStockOrZero() {
        return stockReservado == null ? 0 : stockReservado;
    }

    public boolean isSoldOut() {
        return estadoActual() == EstadoInventario.SIN_STOCK
                || stockDisponible() <= 0;
    }

    public boolean isLowStock() {
        return estadoActual() == EstadoInventario.BAJO_STOCK;
    }

    public Integer dailyCapacityAvailable() {
        return inventoryMode() == TipoManejoInventario.CUPO_DIARIO
                ? stockDisponible()
                : null;
    }

    public boolean isAvailableForSale() {
        EstadoInventario currentStatus = estadoActual();
        return currentStatus != EstadoInventario.DESACTIVADO
                && currentStatus != EstadoInventario.SIN_STOCK
                && stockDisponible() > 0;
    }

    public TipoManejoInventario inventoryMode() {
        return tipoManejoSeguro();
    }

    private void validarDisponibilidad(int cantidad) {
        if (stockDisponible() < cantidad) {
            throw new InsufficientStockException(productoId, cantidad, stockDisponible());
        }
    }

    private void validarReserva(int cantidad) {
        if (reservedStockOrZero() < cantidad) {
            throw new InventoryOperationException(
                    "No hay reserva suficiente para liberar o consumir.");
        }
    }

    private void validarCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new InventoryOperationException("La cantidad debe ser mayor a cero.");
        }
    }

    private void validarDisponibleSinControlDeCantidad() {
        if (!isAvailableWithoutQuantityControl()) {
            throw new InsufficientStockException(productoId, 1, 0);
        }
    }

    private void actualizarEstado() {
        estado = estadoActual();
    }

    public int stockMinimoEfectivo() {
        return stockMinimo == null || stockMinimo <= 0
                ? STOCK_MINIMO_PREDETERMINADO
                : stockMinimo;
    }

    private boolean esBajoStock() {
        return stockDisponible() > 0 && stockDisponible() <= stockMinimoEfectivo();
    }

    private EstadoInventario estadoSeguro() {
        return estado == null ? EstadoInventario.DISPONIBLE : estado;
    }

    private TipoManejoInventario tipoManejoSeguro() {
        return tipoManejo == null ? TipoManejoInventario.STOCK_EXACTO : tipoManejo;
    }
}
