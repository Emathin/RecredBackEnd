package com.recred.backend.domain.services;

import com.recred.backend.domain.exceptions.InventoryOperationException;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.InventoryQuickActionType;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.TipoManejoInventario;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Objects;

public class InventoryQuickActionProcessor {

    private final EnumMap<InventoryQuickActionType, QuickAction> actions;
    private final EnumMap<InventoryQuickActionType, String> defaultReasons;
    private final EnumSet<InventoryQuickActionType> actionsThatRequireQuantity;
    private final EnumSet<InventoryQuickActionType> actionsThatRequirePositiveQuantity;

    public InventoryQuickActionProcessor() {
        this.actions = actions();
        this.defaultReasons = defaultReasons();
        this.actionsThatRequireQuantity = EnumSet.of(
                InventoryQuickActionType.SET_STOCK,
                InventoryQuickActionType.ADD_STOCK,
                InventoryQuickActionType.SUBTRACT_STOCK,
                InventoryQuickActionType.SET_DAILY_CAPACITY
        );
        this.actionsThatRequirePositiveQuantity = EnumSet.of(
                InventoryQuickActionType.ADD_STOCK,
                InventoryQuickActionType.SUBTRACT_STOCK
        );
    }

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

        actionFor(action).apply(inventory, quantity);
    }

    public String reason(InventoryQuickActionType action, String customReason) {
        if (customReason != null && !customReason.isBlank()) {
            return customReason;
        }

        return defaultReasonFor(action);
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
        return actionsThatRequireQuantity.contains(action);
    }

    private boolean requiresPositiveQuantity(InventoryQuickActionType action) {
        return actionsThatRequirePositiveQuantity.contains(action);
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

    private QuickAction actionFor(InventoryQuickActionType action) {
        QuickAction quickAction = actions.get(action);
        if (quickAction == null) {
            throw new InventoryOperationException("La accion no esta configurada.");
        }
        return quickAction;
    }

    private String defaultReasonFor(InventoryQuickActionType action) {
        String reason = defaultReasons.get(Objects.requireNonNull(action));
        if (reason == null) {
            throw new InventoryOperationException("La accion no esta configurada.");
        }
        return reason;
    }

    private EnumMap<InventoryQuickActionType, QuickAction> actions() {
        EnumMap<InventoryQuickActionType, QuickAction> quickActions =
                new EnumMap<>(InventoryQuickActionType.class);
        quickActions.put(InventoryQuickActionType.SET_STOCK, this::setExactStock);
        quickActions.put(InventoryQuickActionType.ADD_STOCK, this::addStock);
        quickActions.put(InventoryQuickActionType.SUBTRACT_STOCK, this::subtractStock);
        quickActions.put(InventoryQuickActionType.MARK_SOLD_OUT,
                (inventory, quantity) -> markSoldOut(inventory));
        quickActions.put(InventoryQuickActionType.SET_AVAILABLE,
                (inventory, quantity) -> setAvailable(inventory));
        quickActions.put(InventoryQuickActionType.SET_UNAVAILABLE,
                (inventory, quantity) -> setUnavailable(inventory));
        quickActions.put(InventoryQuickActionType.SET_DAILY_CAPACITY,
                this::setDailyCapacity);
        return quickActions;
    }

    private EnumMap<InventoryQuickActionType, String> defaultReasons() {
        EnumMap<InventoryQuickActionType, String> reasons =
                new EnumMap<>(InventoryQuickActionType.class);
        reasons.put(InventoryQuickActionType.SET_STOCK, "Stock definido");
        reasons.put(InventoryQuickActionType.ADD_STOCK, "Reposicion de stock");
        reasons.put(InventoryQuickActionType.SUBTRACT_STOCK,
                "Descuento manual de stock");
        reasons.put(InventoryQuickActionType.MARK_SOLD_OUT,
                "Producto marcado como agotado");
        reasons.put(InventoryQuickActionType.SET_AVAILABLE,
                "Producto marcado como disponible");
        reasons.put(InventoryQuickActionType.SET_UNAVAILABLE, "Producto pausado");
        reasons.put(InventoryQuickActionType.SET_DAILY_CAPACITY,
                "Cupo diario definido");
        return reasons;
    }

    @FunctionalInterface
    private interface QuickAction {
        void apply(Inventario inventory, Integer quantity);
    }
}
