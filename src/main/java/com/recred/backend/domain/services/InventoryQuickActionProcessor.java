package com.recred.backend.domain.services;

import com.recred.backend.domain.exceptions.InventoryOperationException;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.InventoryQuickActionType;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.TipoManejoInventario;

public class InventoryQuickActionProcessor {

    public void apply(
            Inventario inventory,
            InventoryQuickActionType action,
            Integer quantity,
            Integer stockMinimo
    ) {
        validate(action, quantity, stockMinimo);

        if (stockMinimo != null) {
            inventory.setStockMinimo(stockMinimo);
        }

        switch (action) {
            case SET_STOCK -> setExactStock(inventory, quantity);
            case ADD_STOCK -> addStock(inventory, quantity);
            case SUBTRACT_STOCK -> subtractStock(inventory, quantity);
            case MARK_SOLD_OUT -> markSoldOut(inventory);
            case SET_AVAILABLE -> setAvailable(inventory);
            case SET_UNAVAILABLE -> setUnavailable(inventory);
            case SET_DAILY_CAPACITY -> setDailyCapacity(inventory, quantity);
        }
    }

    public String reason(InventoryQuickActionType action, String customReason) {
        if (customReason != null && !customReason.isBlank()) {
            return customReason;
        }

        return switch (action) {
            case SET_STOCK -> "Stock definido";
            case ADD_STOCK -> "Reposicion de stock";
            case SUBTRACT_STOCK -> "Descuento manual de stock";
            case MARK_SOLD_OUT -> "Producto marcado como agotado";
            case SET_AVAILABLE -> "Producto marcado como disponible";
            case SET_UNAVAILABLE -> "Producto pausado";
            case SET_DAILY_CAPACITY -> "Cupo diario definido";
        };
    }

    private void setExactStock(Inventario inventory, int quantity) {
        inventory.setTipoManejo(TipoManejoInventario.STOCK_EXACTO);
        inventory.ajustarStock(quantity);
    }

    private void addStock(Inventario inventory, int quantity) {
        inventory.setTipoManejo(TipoManejoInventario.STOCK_EXACTO);
        inventory.ajustarStock(stockActual(inventory) + quantity);
    }

    private void subtractStock(Inventario inventory, int quantity) {
        inventory.setTipoManejo(TipoManejoInventario.STOCK_EXACTO);
        int minimumStock = stockReservado(inventory);
        int nextStock = Math.max(minimumStock, stockActual(inventory) - quantity);
        inventory.ajustarStock(nextStock);
    }

    private void markSoldOut(Inventario inventory) {
        if (inventory.getTipoManejo() == TipoManejoInventario.DISPONIBLE_NO_DISPONIBLE) {
            inventory.setEstado(EstadoInventario.SIN_STOCK);
            return;
        }

        inventory.ajustarStock(stockReservado(inventory));
    }

    private void setAvailable(Inventario inventory) {
        inventory.setTipoManejo(TipoManejoInventario.DISPONIBLE_NO_DISPONIBLE);
        inventory.setEstado(EstadoInventario.DISPONIBLE);
    }

    private void setUnavailable(Inventario inventory) {
        inventory.setTipoManejo(TipoManejoInventario.DISPONIBLE_NO_DISPONIBLE);
        inventory.setEstado(EstadoInventario.SIN_STOCK);
    }

    private void setDailyCapacity(Inventario inventory, int quantity) {
        inventory.setTipoManejo(TipoManejoInventario.CUPO_DIARIO);
        inventory.setCupoMaximoDiario(quantity);
        inventory.ajustarStock(quantity);
    }

    private void validate(
            InventoryQuickActionType action,
            Integer quantity,
            Integer stockMinimo
    ) {
        if (action == null) {
            throw new InventoryOperationException("La accion es requerida.");
        }

        if (stockMinimo != null) {
            validateNonNegative(stockMinimo, "El stock minimo no puede ser negativo.");
        }

        if (requiresQuantity(action)) {
            validateQuantityForAction(action, quantity);
        }
    }

    private void validateQuantityForAction(
            InventoryQuickActionType action,
            Integer quantity
    ) {
        if (quantity == null) {
            throw new InventoryOperationException("La cantidad es requerida.");
        }

        if (quantity < 0) {
            throw new InventoryOperationException("La cantidad no puede ser negativa.");
        }

        if (requiresPositiveQuantity(action) && quantity == 0) {
            throw new InventoryOperationException("La cantidad debe ser mayor a cero.");
        }
    }

    private boolean requiresQuantity(InventoryQuickActionType action) {
        return action == InventoryQuickActionType.SET_STOCK
                || action == InventoryQuickActionType.ADD_STOCK
                || action == InventoryQuickActionType.SUBTRACT_STOCK
                || action == InventoryQuickActionType.SET_DAILY_CAPACITY;
    }

    private boolean requiresPositiveQuantity(InventoryQuickActionType action) {
        return action == InventoryQuickActionType.ADD_STOCK
                || action == InventoryQuickActionType.SUBTRACT_STOCK;
    }

    private void validateNonNegative(int value, String message) {
        if (value < 0) {
            throw new InventoryOperationException(message);
        }
    }

    private int stockActual(Inventario inventory) {
        return inventory.getStockActual() == null ? 0 : inventory.getStockActual();
    }

    private int stockReservado(Inventario inventory) {
        return inventory.getStockReservado() == null ? 0 : inventory.getStockReservado();
    }
}
