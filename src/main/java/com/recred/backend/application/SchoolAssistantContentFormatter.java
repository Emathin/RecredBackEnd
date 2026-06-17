package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.SchoolCalendarEvent;
import com.recred.backend.domain.repositories.ProductRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

class SchoolAssistantContentFormatter {

    private static final String PRODUCT_WITHOUT_NAME = "Producto";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d/M/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ProductRepository productRepository;
    private final SchoolAssistantReplyFormatter replyFormatter;

    SchoolAssistantContentFormatter(
            ProductRepository productRepository,
            SchoolAssistantReplyFormatter replyFormatter
    ) {
        this.productRepository = productRepository;
        this.replyFormatter = replyFormatter;
    }

    List<Map<String, Object>> purchasesData(List<Purchase> purchases) {
        return purchases.stream()
                .map(this::purchaseData)
                .toList();
    }

    Map<String, Object> purchaseData(Purchase purchase) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("compraId", value(purchase.getId()));
        data.put("alumnoId", purchase.getStudent() == null
                ? ""
                : value(purchase.getStudent().getId()));
        data.put("buffetId", value(purchase.getBuffetId()));
        data.put("estado", value(purchase.getStatus()));
        data.put("medioPago", value(purchase.getPaymentMethod()));
        data.put("total", purchase.getTotalAmount());
        data.put("fechaHora", value(purchase.getDate()));
        data.put("codigoRetiro", value(purchase.getWithdrawalCode()));
        data.put("fechaRetiro", value(purchase.getPickupDate()));
        data.put("franjaRetiroId", value(purchase.getPickupSlotId()));
        data.put("items", purchaseItemsData(purchase));
        return data;
    }

    List<Map<String, Object>> patternsData(List<PurchasePattern> patterns) {
        return patterns.stream()
                .map(this::patternData)
                .toList();
    }

    Map<String, Object> purchasePlanData(PurchasePlan plan) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("alumnoId", plan.studentId().toString());
        data.put("buffetId", plan.buffetId().toString());
        data.put("totalEstimado", plan.totalAmount());
        data.put("ocurrencias", plan.occurrences());
        data.put("ultimaFecha", value(plan.lastDate()));
        data.put("items", purchasePlanItemsData(plan.items()));
        return data;
    }

    Map<String, Object> pendingPurchaseData(PendingPurchase pendingPurchase) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("alumnoId", pendingPurchase.studentId().toString());
        data.put("buffetId", pendingPurchase.buffetId().toString());
        data.put("recreo", pendingPurchase.recessTime()
                .map(RecessTime::name)
                .orElse(""));
        data.put("items", purchaseItemRequestsData(pendingPurchase.items()));
        return data;
    }

    List<Map<String, Object>> productsData(List<Producto> products) {
        return products.stream()
                .map(this::productData)
                .toList();
    }

    List<Map<String, Object>> eventsData(List<SchoolCalendarEvent> events) {
        return events.stream()
                .map(this::eventData)
                .toList();
    }

    String purchaseList(List<Purchase> purchases) {
        return replyFormatter.formattedItems(purchases.stream()
                .map(this::purchaseSummary)
                .toList());
    }

    String productsList(List<Producto> products) {
        return replyFormatter.formattedItems(products.stream()
                .map(this::productSummary)
                .toList());
    }

    String eventsList(List<SchoolCalendarEvent> events) {
        return replyFormatter.formattedItems(events.stream()
                .map(this::eventSummary)
                .toList());
    }

    String patternList(List<PurchasePattern> patterns) {
        return replyFormatter.formattedItems(patterns.stream()
                .map(this::patternSummary)
                .toList());
    }

    String purchasePlanItemList(List<PurchasePlanItem> items) {
        return replyFormatter.formattedItems(items.stream()
                .map(this::purchasePlanItemSummary)
                .toList());
    }

    String productName(UUID productId) {
        return productRepository.findById(productId)
                .map(this::productName)
                .orElse(PRODUCT_WITHOUT_NAME);
    }

    String productName(Producto product) {
        return product.getNombre() == null ? PRODUCT_WITHOUT_NAME : product.getNombre();
    }

    BigDecimal price(Producto product) {
        return product.getPrecio() == null ? BigDecimal.ZERO : product.getPrecio();
    }

    String money(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        return "$" + safeAmount.setScale(2, RoundingMode.HALF_UP);
    }

    String recessLabel(RecessTime recessTime) {
        return recessTime.getLabel();
    }

    String value(Object value) {
        return value == null ? "" : value.toString();
    }

    String purchaseStatusLabel(EstadoCompra status) {
        return status == null ? "sin estado" : status.getLabel();
    }

    private List<Map<String, Object>> purchaseItemsData(Purchase purchase) {
        return purchase.getItems() == null
                ? List.of()
                : purchase.getItems().entrySet()
                        .stream()
                        .filter(entry -> isValidPurchaseItem(entry.getKey(), entry.getValue()))
                        .map(entry -> purchaseItemData(entry.getKey(), entry.getValue()))
                        .toList();
    }

    private Map<String, Object> purchaseItemData(Producto product, Integer quantity) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productoId", value(product.getId()));
        data.put("nombre", productName(product));
        data.put("cantidad", quantity);
        data.put("precio", price(product));
        return data;
    }

    private Map<String, Object> patternData(PurchasePattern pattern) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ocurrencias", pattern.occurrences());
        data.put("ultimaFecha", value(pattern.lastDate()));
        data.put("items", purchaseItemRequestsData(pattern.items()));
        return data;
    }

    private List<Map<String, Object>> purchaseItemRequestsData(
            List<PurchaseItemRequest> items
    ) {
        return items.stream()
                .map(this::purchaseItemRequestData)
                .toList();
    }

    private Map<String, Object> purchaseItemRequestData(PurchaseItemRequest item) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productoId", item.productId().toString());
        data.put("cantidad", item.quantity());
        data.put("buffetId", value(item.buffetId()));
        return data;
    }

    private List<Map<String, Object>> purchasePlanItemsData(
            List<PurchasePlanItem> items
    ) {
        return items.stream()
                .map(this::purchasePlanItemData)
                .toList();
    }

    private Map<String, Object> purchasePlanItemData(PurchasePlanItem item) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productoId", item.productId().toString());
        data.put("nombre", item.productName());
        data.put("cantidad", item.quantity());
        data.put("precioUnitario", item.unitPrice());
        data.put("subtotal", item.subtotal());
        return data;
    }

    private Map<String, Object> productData(Producto product) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productoId", value(product.getId()));
        data.put("nombre", productName(product));
        data.put("precio", price(product));
        data.put("stockActual", product.getStockActual());
        data.put("stockReservado", product.getStockReservado());
        data.put("stockDisponible", product.getStockDisponible());
        data.put("estadoInventario", value(product.getEstadoInventario()));
        data.put("tipoManejoInventario", value(product.getTipoManejoInventario()));
        data.put("requierePreparacion", product.getRequierePreparacion());
        return data;
    }

    private Map<String, Object> eventData(SchoolCalendarEvent event) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fecha", value(event.date()));
        data.put("tipo", value(event.type()));
        data.put("descripcion", value(event.description()));
        return data;
    }

    private String productSummary(Producto product) {
        return productName(product)
                + " por "
                + money(price(product))
                + " (disponible: "
                + value(product.getStockDisponible())
                + ")";
    }

    private String eventSummary(SchoolCalendarEvent event) {
        return date(event.date())
                + " - "
                + value(event.description())
                + " ("
                + value(event.type())
                + ")";
    }

    private String purchaseSummary(Purchase purchase) {
        return date(purchase.getDate())
                + " - "
                + itemListFromPurchase(purchase)
                + ". Total: "
                + money(purchase.getTotalAmount())
                + ". Estado: "
                + purchaseStatusLabel(purchase.getStatus());
    }

    private String patternSummary(PurchasePattern pattern) {
        return itemListFromRequests(pattern.items())
                + " - "
                + pattern.occurrences()
                + " veces";
    }

    private String purchasePlanItemSummary(PurchasePlanItem item) {
        return item.quantity() + "x " + item.productName();
    }

    private String itemListFromPurchase(Purchase purchase) {
        return purchase.getItems().entrySet()
                .stream()
                .filter(entry -> isValidPurchaseItem(entry.getKey(), entry.getValue()))
                .map(entry -> entry.getValue() + "x " + productName(entry.getKey()))
                .collect(Collectors.joining(", "));
    }

    private String itemListFromRequests(List<PurchaseItemRequest> items) {
        return items.stream()
                .map(item -> item.quantity() + "x " + productName(item.productId()))
                .collect(Collectors.joining(", "));
    }

    private String date(LocalDateTime dateTime) {
        return dateTime == null ? "sin fecha" : DATE_TIME_FORMAT.format(dateTime);
    }

    private String date(LocalDate date) {
        return date == null ? "sin fecha" : DATE_FORMAT.format(date);
    }

    private boolean isValidPurchaseItem(Producto product, Integer quantity) {
        return product != null
                && product.getId() != null
                && quantity != null
                && quantity > 0;
    }
}
