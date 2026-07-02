package com.recred.backend.domain.services;

import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import com.recred.backend.domain.events.KiosqueroRealtimeChangeKind;
import com.recred.backend.domain.events.KiosqueroRealtimeEventPublisher;
import com.recred.backend.domain.events.KiosqueroRealtimeEventType;
import com.recred.backend.domain.exceptions.InventoryNotFoundException;
import com.recred.backend.domain.exceptions.InventoryOperationException;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.InventoryQuickActionType;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.MovimientoStock;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.models.TipoMovimientoStock;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.StockMovementRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

public class InventoryAvailabilityService {

    private static final String ADVANCE_PURCHASE_RESERVATION =
            "Reserva por compra anticipada";
    private static final String ADVANCE_PURCHASE_RELEASE =
            "Liberacion por cancelacion de compra anticipada";
    private static final String ADVANCE_PURCHASE_CONSUMPTION =
            "Consumo confirmado por entrega de compra anticipada";
    private static final String PRESENTIAL_PURCHASE_CONSUMPTION =
            "Consumo por venta presencial";
    private static final EnumMap<TipoMovimientoStock, KiosqueroRealtimeChangeKind>
            CHANGE_KIND_BY_MOVEMENT_TYPE = movementTypeChangeKinds();
    private static final EnumMap<InventoryQuickActionType, KiosqueroRealtimeChangeKind>
            CHANGE_KIND_BY_QUICK_ACTION = quickActionChangeKinds();

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final KiosqueroRealtimeEventPublisher realtimeEventPublisher;
    private final InventoryQuickActionProcessor quickActionProcessor;

    public InventoryAvailabilityService(
            InventoryRepository inventoryRepository,
            StockMovementRepository stockMovementRepository
    ) {
        this(inventoryRepository, stockMovementRepository, event -> {
        });
    }

    public InventoryAvailabilityService(
            InventoryRepository inventoryRepository,
            StockMovementRepository stockMovementRepository,
            KiosqueroRealtimeEventPublisher realtimeEventPublisher
    ) {
        this.inventoryRepository = inventoryRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.quickActionProcessor = new InventoryQuickActionProcessor();
    }

    public void reserve(UUID buffetId, Map<Producto, Integer> cart) {
        applyToCart(
                buffetId,
                cart,
                TipoMovimientoStock.RESERVA,
                ADVANCE_PURCHASE_RESERVATION,
                Inventario::reservar,
                null
        );
    }

    public void releaseReservation(UUID buffetId, Map<Producto, Integer> cart) {
        releaseReservation(buffetId, cart, null);
    }

    public void releaseReservation(UUID buffetId, Map<Producto, Integer> cart, UUID purchaseId) {
        applyToCart(
                buffetId,
                cart,
                TipoMovimientoStock.LIBERACION,
                ADVANCE_PURCHASE_RELEASE,
                Inventario::liberarReserva,
                purchaseId
        );
    }

    public void consumeReservation(UUID buffetId, Map<Producto, Integer> cart) {
        consumeReservation(buffetId, cart, null);
    }

    public void consumeReservation(UUID buffetId, Map<Producto, Integer> cart, UUID purchaseId) {
        applyToCart(
                buffetId,
                cart,
                TipoMovimientoStock.CONSUMO,
                ADVANCE_PURCHASE_CONSUMPTION,
                Inventario::consumirReserva,
                purchaseId
        );
    }

    public void consumeDirectly(UUID buffetId, Map<Producto, Integer> cart) {
        applyToCart(
                buffetId,
                cart,
                TipoMovimientoStock.VENTA,
                PRESENTIAL_PURCHASE_CONSUMPTION,
                Inventario::consumir,
                null
        );
    }

    public Inventario adjustStock(
            UUID buffetId,
            UUID productId,
            int newStock,
            String reason,
            UUID userId
    ) {
        Inventario inventory = lockedInventory(buffetId, productId);
        int previousAvailable = inventory.stockDisponible();
        inventory.ajustarStock(newStock);
        Inventario savedInventory = inventoryRepository.save(inventory);
        registerMovement(
                savedInventory,
                TipoMovimientoStock.AJUSTE,
                Math.abs(savedInventory.stockDisponible() - previousAvailable),
                previousAvailable,
                savedInventory.stockDisponible(),
                reason,
                userId,
                null
        );
        publishInventoryEvents(
                savedInventory,
                TipoMovimientoStock.AJUSTE,
                KiosqueroRealtimeChangeKind.STOCK_ONLY,
                reason);
        return savedInventory;
    }

    public Inventario adjustInventory(
            UUID buffetId,
            UUID productId,
            Integer newStock,
            Integer stockMinimo,
            EstadoInventario estado,
            TipoManejoInventario tipoManejo,
            Boolean disponible,
            Integer cupoMaximoDiario,
            String reason,
            UUID userId
    ) {
        Inventario inventory = lockedInventory(buffetId, productId);
        int previousAvailable = inventory.stockDisponible();
        applyInventorySettings(
                inventory,
                newStock,
                stockMinimo,
                estado,
                tipoManejo,
                disponible,
                cupoMaximoDiario);
        Inventario savedInventory = inventoryRepository.save(inventory);
        registerMovement(
                savedInventory,
                TipoMovimientoStock.AJUSTE,
                Math.abs(savedInventory.stockDisponible() - previousAvailable),
                previousAvailable,
                savedInventory.stockDisponible(),
                reason,
                userId,
                null
        );
        publishInventoryEvents(
                savedInventory,
                TipoMovimientoStock.AJUSTE,
                KiosqueroRealtimeChangeKind.CONFIG_CHANGED,
                reason);
        return savedInventory;
    }

    public Inventario applyQuickAction(
            UUID buffetId,
            UUID productId,
            InventoryQuickActionType action,
            Integer quantity,
            Integer stockMinimo,
            String reason,
            UUID userId
    ) {
        Inventario inventory = inventoryForQuickAction(buffetId, productId, action);
        int previousAvailable = inventory.stockDisponible();
        quickActionProcessor.apply(inventory, action, quantity, stockMinimo);
        Inventario savedInventory = inventoryRepository.save(inventory);
        String movementReason = quickActionProcessor.reason(action, reason);
        registerMovement(
                savedInventory,
                TipoMovimientoStock.AJUSTE,
                Math.abs(savedInventory.stockDisponible() - previousAvailable),
                previousAvailable,
                savedInventory.stockDisponible(),
                movementReason,
                userId,
                null
        );
        publishInventoryEvents(
                savedInventory,
                TipoMovimientoStock.AJUSTE,
                changeKind(action),
                movementReason);
        return savedInventory;
    }

    private void applyToCart(
            UUID buffetId,
            Map<Producto, Integer> cart,
            TipoMovimientoStock movementType,
            String reason,
            BiConsumer<Inventario, Integer> operation,
            UUID purchaseId
    ) {
        validateCart(buffetId, cart);
        sortedItems(cart).forEach(item -> applyToInventory(
                buffetId,
                item.product(),
                item.quantity(),
                movementType,
                reason,
                operation,
                purchaseId
        ));
    }

    private void applyToInventory(
            UUID buffetId,
            Producto product,
            int quantity,
            TipoMovimientoStock movementType,
            String reason,
            BiConsumer<Inventario, Integer> operation,
            UUID purchaseId
    ) {
        Inventario inventory = lockedInventory(buffetId, product.getId());
        int previousAvailable = inventory.stockDisponible();
        operation.accept(inventory, quantity);
        Inventario savedInventory = inventoryRepository.save(inventory);
        registerMovement(
                savedInventory,
                movementType,
                quantity,
                previousAvailable,
                savedInventory.stockDisponible(),
                reason,
                null,
                purchaseId);
        publishInventoryEvents(
                savedInventory,
                movementType,
                changeKind(movementType),
                reason);
    }

    private void applyInventorySettings(
            Inventario inventory,
            Integer newStock,
            Integer stockMinimo,
            EstadoInventario estado,
            TipoManejoInventario tipoManejo,
            Boolean disponible,
            Integer cupoMaximoDiario
    ) {
        if (tipoManejo != null) {
            inventory.setTipoManejo(tipoManejo);
        }

        if (stockMinimo != null) {
            validateNonNegative(stockMinimo, "El stock minimo no puede ser negativo.");
            inventory.setStockMinimo(stockMinimo);
        }

        if (cupoMaximoDiario != null) {
            validateNonNegative(cupoMaximoDiario, "El cupo diario no puede ser negativo.");
            inventory.setCupoMaximoDiario(cupoMaximoDiario);
        }

        if (newStock != null) {
            inventory.ajustarStock(newStock);
        } else if (estado == null && disponible == null) {
            inventory.ajustarStock(inventory.getStockActual() == null
                    ? 0
                    : inventory.getStockActual());
        }

        if (estado != null) {
            inventory.setEstado(estado);
        }

        if (disponible != null) {
            inventory.setEstado(disponible ? EstadoInventario.DISPONIBLE
                    : EstadoInventario.SIN_STOCK);
        }
    }

    private Inventario lockedInventory(UUID buffetId, UUID productId) {
        if (buffetId == null) {
            throw new InventoryOperationException("El buffet es requerido.");
        }

        if (productId == null) {
            throw new InventoryOperationException("El producto es requerido.");
        }

        return inventoryRepository.findLockedByBuffetAndProduct(buffetId, productId)
                .orElseThrow(() -> new InventoryNotFoundException(buffetId, productId));
    }

    private Inventario inventoryForQuickAction(
            UUID buffetId,
            UUID productId,
            InventoryQuickActionType action
    ) {
        if (buffetId == null) {
            throw new InventoryOperationException("El buffet es requerido.");
        }

        if (productId == null) {
            throw new InventoryOperationException("El producto es requerido.");
        }

        if (requiresExistingInventory(action)) {
            return lockedInventory(buffetId, productId);
        }

        return inventoryRepository.findLockedByBuffetAndProduct(buffetId, productId)
                .orElseGet(() -> newInventory(buffetId, productId));
    }

    private boolean requiresExistingInventory(InventoryQuickActionType action) {
        return action == InventoryQuickActionType.MARK_SOLD_OUT
                || action == InventoryQuickActionType.SET_UNAVAILABLE;
    }

    private Inventario newInventory(UUID buffetId, UUID productId) {
        Inventario inventory = new Inventario();
        inventory.setBuffetId(buffetId);
        inventory.setProductoId(productId);
        inventory.setStockActual(0);
        inventory.setStockReservado(0);
        inventory.setStockMinimo(Inventario.STOCK_MINIMO_PREDETERMINADO);
        inventory.setEstado(EstadoInventario.DISPONIBLE);
        inventory.setTipoManejo(TipoManejoInventario.STOCK_EXACTO);
        return inventory;
    }

    private void registerMovement(
            Inventario inventory,
            TipoMovimientoStock type,
            int quantity,
            Integer previousQuantity,
            Integer newQuantity,
            String reason,
            UUID userId,
            UUID purchaseId
    ) {
        if (quantity <= 0) {
            return;
        }

        if (inventory.getId() == null) {
            throw new InventoryOperationException("El inventario no tiene identificador.");
        }

        stockMovementRepository.save(new MovimientoStock(
                null,
                inventory.getId(),
                type,
                quantity,
                previousQuantity,
                newQuantity,
                reason,
                userId,
                purchaseId,
                null
        ));
    }

    private void validateNonNegative(int value, String message) {
        if (value < 0) {
            throw new InventoryOperationException(message);
        }
    }

    private void publishInventoryEvents(
            Inventario inventory,
            TipoMovimientoStock movementType,
            KiosqueroRealtimeChangeKind changeKind,
            String reason
    ) {
        realtimeEventPublisher.publish(KiosqueroRealtimeEvent.inventory(
                KiosqueroRealtimeEventType.STOCK_CHANGED,
                inventory,
                movementType,
                changeKind,
                reason,
                "Stock actualizado"
        ));

        publishInventoryStatusEvent(inventory, movementType, reason);
        publishDashboardChanged(inventory.getBuffetId(), changeKind, reason);
    }

    private void publishInventoryStatusEvent(
            Inventario inventory,
            TipoMovimientoStock movementType,
            String reason
    ) {
        if (inventory.isSoldOut()) {
            publishSoldOutEvent(inventory, movementType, reason);
            return;
        }

        publishLowStockEventIfNeeded(inventory, movementType, reason);
    }

    private void publishSoldOutEvent(
            Inventario inventory,
            TipoMovimientoStock movementType,
            String reason
    ) {
        realtimeEventPublisher.publish(KiosqueroRealtimeEvent.inventory(
                KiosqueroRealtimeEventType.PRODUCT_SOLD_OUT,
                inventory,
                movementType,
                KiosqueroRealtimeChangeKind.SOLD_OUT,
                reason,
                "Producto sin stock"
        ));
    }

    private void publishLowStockEventIfNeeded(
            Inventario inventory,
            TipoMovimientoStock movementType,
            String reason
    ) {
        Optional.of(inventory)
                .filter(Inventario::isLowStock)
                .ifPresent(lowStockInventory -> publishLowStockEvent(
                        lowStockInventory,
                        movementType,
                        reason));
    }

    private void publishLowStockEvent(
            Inventario inventory,
            TipoMovimientoStock movementType,
            String reason
    ) {
        realtimeEventPublisher.publish(KiosqueroRealtimeEvent.inventory(
                KiosqueroRealtimeEventType.LOW_STOCK,
                inventory,
                movementType,
                KiosqueroRealtimeChangeKind.LOW_STOCK,
                reason,
                "Stock bajo"
        ));
    }

    private void publishDashboardChanged(
            UUID buffetId,
            KiosqueroRealtimeChangeKind changeKind,
            String reason
    ) {
        realtimeEventPublisher.publish(KiosqueroRealtimeEvent.dashboardChanged(
                buffetId,
                LocalDate.now(),
                changeKind,
                reason
        ));
    }

    private KiosqueroRealtimeChangeKind changeKind(TipoMovimientoStock movementType) {
        return CHANGE_KIND_BY_MOVEMENT_TYPE.getOrDefault(
                movementType,
                KiosqueroRealtimeChangeKind.STOCK_ONLY);
    }

    private KiosqueroRealtimeChangeKind changeKind(InventoryQuickActionType action) {
        return Optional.ofNullable(CHANGE_KIND_BY_QUICK_ACTION.get(action))
                .orElseThrow(() -> new InventoryOperationException(
                        "La accion no esta configurada."));
    }

    private static EnumMap<TipoMovimientoStock, KiosqueroRealtimeChangeKind>
            movementTypeChangeKinds() {
        EnumMap<TipoMovimientoStock, KiosqueroRealtimeChangeKind> changeKinds =
                new EnumMap<>(TipoMovimientoStock.class);
        changeKinds.put(TipoMovimientoStock.VENTA, KiosqueroRealtimeChangeKind.SALE);
        changeKinds.put(TipoMovimientoStock.RESERVA,
                KiosqueroRealtimeChangeKind.RESERVATION);
        changeKinds.put(TipoMovimientoStock.LIBERACION,
                KiosqueroRealtimeChangeKind.RESERVATION_RELEASED);
        changeKinds.put(TipoMovimientoStock.CONSUMO,
                KiosqueroRealtimeChangeKind.RESERVATION_CONSUMED);
        return changeKinds;
    }

    private static EnumMap<InventoryQuickActionType, KiosqueroRealtimeChangeKind>
            quickActionChangeKinds() {
        EnumMap<InventoryQuickActionType, KiosqueroRealtimeChangeKind> changeKinds =
                new EnumMap<>(InventoryQuickActionType.class);
        changeKinds.put(InventoryQuickActionType.SET_DAILY_CAPACITY,
                KiosqueroRealtimeChangeKind.CONFIG_CHANGED);
        changeKinds.put(InventoryQuickActionType.SET_AVAILABLE,
                KiosqueroRealtimeChangeKind.CONFIG_CHANGED);
        changeKinds.put(InventoryQuickActionType.SET_UNAVAILABLE,
                KiosqueroRealtimeChangeKind.CONFIG_CHANGED);
        changeKinds.put(InventoryQuickActionType.SET_STOCK,
                KiosqueroRealtimeChangeKind.STOCK_ONLY);
        changeKinds.put(InventoryQuickActionType.ADD_STOCK,
                KiosqueroRealtimeChangeKind.STOCK_ONLY);
        changeKinds.put(InventoryQuickActionType.SUBTRACT_STOCK,
                KiosqueroRealtimeChangeKind.STOCK_ONLY);
        changeKinds.put(InventoryQuickActionType.MARK_SOLD_OUT,
                KiosqueroRealtimeChangeKind.STOCK_ONLY);
        return changeKinds;
    }

    private List<CartItem> sortedItems(Map<Producto, Integer> cart) {
        return cart.entrySet()
                .stream()
                .map(entry -> new CartItem(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(item -> item.product().getId()))
                .toList();
    }

    private void validateCart(UUID buffetId, Map<Producto, Integer> cart) {
        if (buffetId == null) {
            throw new InventoryOperationException("El buffet es requerido.");
        }

        if (cart == null || cart.isEmpty()) {
            throw new InventoryOperationException("El carrito no puede estar vacio.");
        }

        sortedItems(cart).forEach(this::validateCartItem);
    }

    private void validateCartItem(CartItem item) {
        if (item.product() == null || item.product().getId() == null) {
            throw new InventoryOperationException("El producto es requerido.");
        }

        if (item.quantity() == null || item.quantity() <= 0) {
            throw new InventoryOperationException("La cantidad debe ser mayor a cero.");
        }
    }

    private record CartItem(Producto product, Integer quantity) {
    }
}
