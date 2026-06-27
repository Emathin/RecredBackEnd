package com.recred.backend.application;

import com.recred.backend.application.GetKiosqueroDashboardResponse.DashboardInventoryAttention;
import com.recred.backend.application.GetKiosqueroDashboardResponse.DashboardProductMetric;
import com.recred.backend.application.GetKiosqueroDashboardResponse.DashboardProducts;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class DashboardProductsCalculator {

    private static final int TOP_PRODUCTS_LIMIT = 5;

    DashboardProducts calculate(
            List<Purchase> purchases,
            List<Producto> inventory,
            boolean rangeMode
    ) {
        return new DashboardProducts(
                topSoldProducts(purchases),
                mostReservedProducts(purchases, inventory, rangeMode),
                productsNeedingRestock(inventory),
                soldOutProducts(inventory)
        );
    }

    private List<DashboardProductMetric> topSoldProducts(List<Purchase> purchases) {
        Map<UUID, ProductAccumulator> products = new LinkedHashMap<>();
        purchases.stream()
                .filter(Purchase::isDelivered)
                .forEach(purchase -> addProducts(products, purchase));
        return productMetrics(products);
    }

    private List<DashboardProductMetric> mostReservedProducts(
            List<Purchase> purchases,
            List<Producto> inventory,
            boolean rangeMode
    ) {
        if (rangeMode) {
            return mostReservedPurchaseProducts(purchases);
        }

        return mostReservedInventoryProducts(inventory);
    }

    private List<DashboardProductMetric> mostReservedInventoryProducts(
            List<Producto> inventory
    ) {
        return inventory.stream()
                .filter(product -> product.reservedStockOrZero() > 0)
                .map(product -> new DashboardProductMetric(
                        product.getId(),
                        product.getNombre(),
                        product.reservedStockOrZero(),
                        BigDecimal.ZERO,
                        product.getUrlImagen()
                ))
                .sorted(productMetricComparator())
                .limit(TOP_PRODUCTS_LIMIT)
                .toList();
    }

    private List<DashboardProductMetric> mostReservedPurchaseProducts(
            List<Purchase> purchases
    ) {
        Map<UUID, ProductAccumulator> products = new LinkedHashMap<>();
        purchases.stream()
                .filter(Purchase::isAdvancePurchase)
                .forEach(purchase -> addProducts(products, purchase));
        return productMetrics(products);
    }

    private List<DashboardProductMetric> productMetrics(
            Map<UUID, ProductAccumulator> products
    ) {
        return products.values().stream()
                .map(ProductAccumulator::toMetric)
                .sorted(productMetricComparator())
                .limit(TOP_PRODUCTS_LIMIT)
                .toList();
    }

    private List<DashboardInventoryAttention> productsNeedingRestock(
            List<Producto> inventory
    ) {
        return inventory.stream()
                .filter(Producto::needsRestock)
                .map(this::toInventoryAttention)
                .sorted(inventoryAttentionComparator())
                .limit(TOP_PRODUCTS_LIMIT)
                .toList();
    }

    private List<DashboardInventoryAttention> soldOutProducts(List<Producto> inventory) {
        return inventory.stream()
                .filter(Producto::isSoldOut)
                .map(this::toInventoryAttention)
                .sorted(inventoryAttentionComparator())
                .limit(TOP_PRODUCTS_LIMIT)
                .toList();
    }

    private void addProducts(
            Map<UUID, ProductAccumulator> products,
            Purchase purchase
    ) {
        purchase.getItems().forEach((product, quantity) ->
                addProduct(products, product, quantity));
    }

    private void addProduct(
            Map<UUID, ProductAccumulator> products,
            Producto product,
            Integer quantity
    ) {
        if (invalidProductQuantity(product, quantity)) {
            return;
        }

        products.computeIfAbsent(
                product.getId(),
                id -> new ProductAccumulator(product)
        ).add(quantity, product.priceOrZero());
    }

    private boolean invalidProductQuantity(Producto product, Integer quantity) {
        return product == null || product.getId() == null || invalidQuantity(quantity);
    }

    private boolean invalidQuantity(Integer quantity) {
        return quantity == null || quantity <= 0;
    }

    private DashboardInventoryAttention toInventoryAttention(Producto product) {
        return new DashboardInventoryAttention(
                product.getId(),
                product.getNombre(),
                product.getStockDisponible(),
                product.getStockMinimo(),
                product.getEstadoInventario() == null
                        ? null
                        : product.getEstadoInventario().name(),
                product.getUrlImagen()
        );
    }

    private Comparator<DashboardProductMetric> productMetricComparator() {
        return Comparator
                .comparingInt(DashboardProductMetric::quantity)
                .reversed()
                .thenComparing(DashboardProductMetric::productName);
    }

    private Comparator<DashboardInventoryAttention> inventoryAttentionComparator() {
        return Comparator
                .comparingInt((DashboardInventoryAttention item) ->
                        safeQuantity(item.stockDisponible()))
                .thenComparing(DashboardInventoryAttention::productName);
    }

    private int safeQuantity(Integer quantity) {
        return quantity == null ? 0 : quantity;
    }

    private static class ProductAccumulator {
        private final UUID productId;
        private final String productName;
        private final String productImage;
        private int quantity;
        private BigDecimal total = BigDecimal.ZERO;

        ProductAccumulator(Producto product) {
            this.productId = product.getId();
            this.productName = product.getNombre();
            this.productImage = product.getUrlImagen();
        }

        void add(int quantityToAdd, BigDecimal unitPrice) {
            quantity += quantityToAdd;
            total = total.add(unitPrice.multiply(BigDecimal.valueOf(quantityToAdd)));
        }

        DashboardProductMetric toMetric() {
            return new DashboardProductMetric(
                    productId,
                    productName,
                    quantity,
                    total,
                    productImage
            );
        }
    }
}
