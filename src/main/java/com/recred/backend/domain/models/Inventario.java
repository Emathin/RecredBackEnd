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
    private LocalDateTime actualizadoEn;

    public int stockDisponible() {
        if (!requiereControlDeCantidad()) {
            return estaDisponibleSinControlDeCantidad() ? 1 : 0;
        }

        return Math.max(0, stockActualSeguro() - stockReservadoSeguro());
    }

    public void reservar(int cantidad) {
        validarCantidad(cantidad);

        if (!requiereControlDeCantidad()) {
            validarDisponibleSinControlDeCantidad();
            return;
        }

        validarDisponibilidad(cantidad);
        stockReservado = stockReservadoSeguro() + cantidad;
        actualizarEstado();
    }

    public void consumir(int cantidad) {
        validarCantidad(cantidad);

        if (!requiereControlDeCantidad()) {
            validarDisponibleSinControlDeCantidad();
            return;
        }

        validarDisponibilidad(cantidad);
        stockActual = stockActualSeguro() - cantidad;
        actualizarEstado();
    }

    public void consumirReserva(int cantidad) {
        validarCantidad(cantidad);

        if (!requiereControlDeCantidad()) {
            validarDisponibleSinControlDeCantidad();
            return;
        }

        validarReserva(cantidad);
        stockActual = stockActualSeguro() - cantidad;
        stockReservado = stockReservadoSeguro() - cantidad;
        actualizarEstado();
    }

    public void liberarReserva(int cantidad) {
        validarCantidad(cantidad);

        if (!requiereControlDeCantidad()) {
            return;
        }

        validarReserva(cantidad);
        stockReservado = stockReservadoSeguro() - cantidad;
        actualizarEstado();
    }

    public void ajustarStock(int nuevoStock) {
        if (nuevoStock < 0) {
            throw new InventoryOperationException("El stock no puede ser negativo.");
        }

        if (requiereControlDeCantidad() && nuevoStock < stockReservadoSeguro()) {
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
        return requiereControlDeCantidad()
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

    private boolean requiereControlDeCantidad() {
        return tipoManejoSeguro() != TipoManejoInventario.DISPONIBLE_NO_DISPONIBLE;
    }

    private void validarDisponibilidad(int cantidad) {
        if (stockDisponible() < cantidad) {
            throw new InsufficientStockException(productoId, cantidad, stockDisponible());
        }
    }

    private void validarReserva(int cantidad) {
        if (stockReservadoSeguro() < cantidad) {
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
        if (!estaDisponibleSinControlDeCantidad()) {
            throw new InsufficientStockException(productoId, 1, 0);
        }
    }

    private boolean estaDisponibleSinControlDeCantidad() {
        return estadoSeguro() != EstadoInventario.SIN_STOCK
                && estadoSeguro() != EstadoInventario.DESACTIVADO;
    }

    private void actualizarEstado() {
        estado = estadoActual();
    }

    private int stockActualSeguro() {
        return stockActual == null ? 0 : stockActual;
    }

    private int stockReservadoSeguro() {
        return stockReservado == null ? 0 : stockReservado;
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
