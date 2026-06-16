package com.recred.backend.domain.models;

import com.recred.backend.domain.exceptions.InsufficientStockException;
import com.recred.backend.domain.exceptions.InventoryOperationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventarioTest {

    @Test
    void reservar_CuandoHayStock_DeberiaIncrementarReservado() {
        Inventario inventario = inventory(10, 2);

        inventario.reservar(3);

        assertEquals(5, inventario.getStockReservado());
        assertEquals(5, inventario.stockDisponible());
    }

    @Test
    void reservar_CuandoNoHayStock_DeberiaLanzarExcepcion() {
        Inventario inventario = inventory(4, 2);

        assertThrows(InsufficientStockException.class, () -> inventario.reservar(3));
    }

    @Test
    void consumir_DeberiaDescontarStockFisico() {
        Inventario inventario = inventory(10, 2);

        inventario.consumir(3);

        assertEquals(7, inventario.getStockActual());
        assertEquals(5, inventario.stockDisponible());
    }

    @Test
    void consumirReserva_DeberiaDescontarStockYReserva() {
        Inventario inventario = inventory(10, 4);

        inventario.consumirReserva(3);

        assertEquals(7, inventario.getStockActual());
        assertEquals(1, inventario.getStockReservado());
        assertEquals(6, inventario.stockDisponible());
    }

    @Test
    void liberarReserva_DeberiaReducirReservado() {
        Inventario inventario = inventory(10, 4);

        inventario.liberarReserva(3);

        assertEquals(1, inventario.getStockReservado());
        assertEquals(9, inventario.stockDisponible());
    }

    @Test
    void productoDisponibleNoDisponible_NoDebeDescontarCantidad() {
        Inventario inventario = inventory(0, 0);
        inventario.setTipoManejo(TipoManejoInventario.DISPONIBLE_NO_DISPONIBLE);
        inventario.setEstado(EstadoInventario.DISPONIBLE);

        inventario.reservar(10);
        inventario.consumir(10);

        assertEquals(0, inventario.getStockActual());
        assertEquals(0, inventario.getStockReservado());
        assertEquals(1, inventario.stockDisponible());
    }

    @Test
    void productoDisponibleNoDisponible_DeberiaConsumirReservaSinDescontarCantidad() {
        Inventario inventario = inventory(0, 0);
        inventario.setTipoManejo(TipoManejoInventario.DISPONIBLE_NO_DISPONIBLE);
        inventario.setEstado(EstadoInventario.DISPONIBLE);

        inventario.consumirReserva(1);

        assertEquals(0, inventario.getStockActual());
        assertEquals(0, inventario.getStockReservado());
        assertEquals(1, inventario.stockDisponible());
    }

    @Test
    void liberarReserva_ConProductoDisponibleNoDisponible_NoDebeCambiarStock() {
        Inventario inventario = inventory(0, 0);
        inventario.setTipoManejo(TipoManejoInventario.DISPONIBLE_NO_DISPONIBLE);
        inventario.setEstado(EstadoInventario.DISPONIBLE);

        inventario.liberarReserva(1);

        assertEquals(0, inventario.getStockActual());
        assertEquals(0, inventario.getStockReservado());
    }

    @Test
    void productoDisponibleNoDisponible_CuandoNoEstaDisponible_DeberiaLanzarExcepcion() {
        Inventario inventario = inventory(0, 0);
        inventario.setTipoManejo(TipoManejoInventario.DISPONIBLE_NO_DISPONIBLE);
        inventario.setEstado(EstadoInventario.SIN_STOCK);

        assertThrows(InsufficientStockException.class, () -> inventario.consumirReserva(1));
    }

    @Test
    void ajustarStock_MenorAReservado_DeberiaLanzarExcepcion() {
        Inventario inventario = inventory(10, 4);

        assertThrows(InventoryOperationException.class, () -> inventario.ajustarStock(3));
    }

    @Test
    void ajustarStock_Negativo_DeberiaLanzarExcepcion() {
        Inventario inventario = inventory(10, 0);

        assertThrows(InventoryOperationException.class, () -> inventario.ajustarStock(-1));
    }

    @Test
    void estadoActual_CuandoEstaDesactivado_DeberiaMantenerDesactivado() {
        Inventario inventario = inventory(10, 0);
        inventario.setEstado(EstadoInventario.DESACTIVADO);

        assertEquals(EstadoInventario.DESACTIVADO, inventario.estadoActual());
    }

    @Test
    void liberarReserva_CuandoReservaEsInsuficiente_DeberiaLanzarExcepcion() {
        Inventario inventario = inventory(10, 1);

        assertThrows(InventoryOperationException.class, () -> inventario.liberarReserva(2));
    }

    @Test
    void consumirReserva_CuandoReservaEsInsuficiente_DeberiaLanzarExcepcion() {
        Inventario inventario = inventory(10, 1);

        assertThrows(InventoryOperationException.class, () -> inventario.consumirReserva(2));
    }

    @Test
    void operacionesConCantidadCero_DeberianLanzarExcepcion() {
        Inventario inventario = inventory(10, 1);

        assertThrows(InventoryOperationException.class, () -> inventario.reservar(0));
        assertThrows(InventoryOperationException.class, () -> inventario.consumir(0));
        assertThrows(InventoryOperationException.class, () -> inventario.consumirReserva(0));
        assertThrows(InventoryOperationException.class, () -> inventario.liberarReserva(0));
    }

    @Test
    void ajustarStock_CuandoNoTieneStockMinimoYQuedanCuatro_DeberiaQuedarBajoStock() {
        Inventario inventario = inventory(10, 0);
        inventario.setStockMinimo(null);

        inventario.ajustarStock(4);

        assertEquals(EstadoInventario.BAJO_STOCK, inventario.getEstado());
    }

    @Test
    void ajustarStock_CuandoTieneStockMinimoCeroYQuedanCuatro_DeberiaUsarDefault() {
        Inventario inventario = inventory(10, 0);
        inventario.setStockMinimo(0);

        inventario.ajustarStock(4);

        assertEquals(EstadoInventario.BAJO_STOCK, inventario.getEstado());
        assertEquals(Inventario.STOCK_MINIMO_PREDETERMINADO,
                inventario.stockMinimoEfectivo());
    }

    @Test
    void ajustarStock_CuandoNoTieneStockMinimoYQuedanCinco_DeberiaQuedarDisponible() {
        Inventario inventario = inventory(10, 0);
        inventario.setStockMinimo(null);

        inventario.ajustarStock(5);

        assertEquals(EstadoInventario.DISPONIBLE, inventario.getEstado());
    }

    @Test
    void ajustarStock_CuandoNoHayStock_DeberiaQuedarSinStock() {
        Inventario inventario = inventory(10, 0);
        inventario.setStockMinimo(null);

        inventario.ajustarStock(0);

        assertEquals(EstadoInventario.SIN_STOCK, inventario.getEstado());
    }

    @Test
    void ajustarStock_CuandoTieneStockMinimoConfigurado_DeberiaRespetarlo() {
        Inventario inventario = inventory(10, 0);
        inventario.setStockMinimo(2);

        inventario.ajustarStock(3);

        assertEquals(EstadoInventario.DISPONIBLE, inventario.getEstado());
    }

    private Inventario inventory(int stockActual, int stockReservado) {
        Inventario inventario = new Inventario();
        inventario.setId(UUID.randomUUID());
        inventario.setBuffetId(UUID.randomUUID());
        inventario.setProductoId(UUID.randomUUID());
        inventario.setStockActual(stockActual);
        inventario.setStockReservado(stockReservado);
        inventario.setStockMinimo(1);
        inventario.setEstado(EstadoInventario.DISPONIBLE);
        inventario.setTipoManejo(TipoManejoInventario.STOCK_EXACTO);
        return inventario;
    }
}
