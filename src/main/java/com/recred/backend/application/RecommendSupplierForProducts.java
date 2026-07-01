package com.recred.backend.application;

import com.recred.backend.application.RecommendSupplierForProductsResponse.Alternative;
import com.recred.backend.application.RecommendSupplierForProductsResponse.Recommendation;
import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import com.recred.backend.domain.models.ListaPrecioProveedor;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ItemListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.ProveedorRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class RecommendSupplierForProducts {

    private static final String UNKNOWN_SUPPLIER = "Unknown";
    private static final String PRODUCT_NOT_FOUND = "Product not found";

    private final ItemListaPrecioProveedorRepository itemRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductRepository productRepository;
    private final SupplierUnitQuantityParser quantityParser;

    public RecommendSupplierForProducts(
            ItemListaPrecioProveedorRepository itemRepository,
            ProveedorRepository proveedorRepository,
            ProductRepository productRepository
    ) {
        this.itemRepository = itemRepository;
        this.proveedorRepository = proveedorRepository;
        this.productRepository = productRepository;
        this.quantityParser = new SupplierUnitQuantityParser();
    }

    @Transactional(readOnly = true)
    public RecommendSupplierForProductsResponse execute(
            RecommendSupplierForProductsRequest request
    ) {
        if (!request.hasProducts()) {
            return RecommendSupplierForProductsResponse.empty();
        }

        SupplierIndex supplierIndex = supplierIndex(request.kioskOwnerId());
        Map<UUID, List<ItemListaPrecioProveedor>> itemsByProduct =
                itemsByProduct(request);
        List<Recommendation> recommendations = request.productIdsOrEmpty()
                .stream()
                .map(productId -> recommendationFor(
                        productId,
                        itemsByProduct.getOrDefault(productId, List.of()),
                        supplierIndex
                ))
                .toList();

        return new RecommendSupplierForProductsResponse(recommendations);
    }

    private SupplierIndex supplierIndex(UUID kioskOwnerId) {
        Map<UUID, String> supplierNames = new HashMap<>();
        Map<UUID, UUID> priceListSupplierIds = new HashMap<>();

        proveedorRepository.findByKiosqueroId(kioskOwnerId)
                .forEach(supplier -> addSupplier(supplier, supplierNames, priceListSupplierIds));

        return new SupplierIndex(supplierNames, priceListSupplierIds);
    }

    private void addSupplier(
            Proveedor supplier,
            Map<UUID, String> supplierNames,
            Map<UUID, UUID> priceListSupplierIds
    ) {
        supplierNames.put(supplier.getId(), supplier.getNombre());
        supplier.priceListsOrEmpty()
                .stream()
                .map(ListaPrecioProveedor::getId)
                .forEach(priceListId -> priceListSupplierIds.put(priceListId, supplier.getId()));
    }

    private Map<UUID, List<ItemListaPrecioProveedor>> itemsByProduct(
            RecommendSupplierForProductsRequest request
    ) {
        Map<UUID, List<ItemListaPrecioProveedor>> itemsByProduct =
                emptyProductMap(request.productIdsOrEmpty());
        itemRepository.findItemsActivosByKiosqueroIdAndProductoIds(
                        request.kioskOwnerId(),
                        request.productIdsOrEmpty()
                )
                .stream()
                .filter(ItemListaPrecioProveedor::hasInventoryProduct)
                .filter(item -> itemsByProduct.containsKey(item.getProductoInventarioId()))
                .forEach(item -> itemsByProduct.get(item.getProductoInventarioId()).add(item));
        return itemsByProduct;
    }

    private Map<UUID, List<ItemListaPrecioProveedor>> emptyProductMap(List<UUID> productIds) {
        Map<UUID, List<ItemListaPrecioProveedor>> products = new LinkedHashMap<>();
        productIds.forEach(productId -> products.put(productId, new ArrayList<>()));
        return products;
    }

    private Recommendation recommendationFor(
            UUID productId,
            List<ItemListaPrecioProveedor> items,
            SupplierIndex supplierIndex
    ) {
        if (items.isEmpty()) {
            return emptyRecommendation(productId);
        }

        List<ItemListaPrecioProveedor> sortedItems = sortedByUnitPrice(items);
        ItemListaPrecioProveedor recommendedItem = sortedItems.get(0);
        UUID recommendedSupplierId = supplierIndex.supplierIdFor(
                recommendedItem.getListaPrecioId()
        );

        return new Recommendation(
                productId,
                recommendedItem.getNombreProductoInventario(),
                recommendedSupplierId,
                supplierIndex.nameFor(recommendedSupplierId),
                recommendedItem.getPrecio(),
                recommendedItem.getUnidad(),
                unitPrice(recommendedItem),
                alternatives(sortedItems, supplierIndex)
        );
    }

    private Recommendation emptyRecommendation(UUID productId) {
        return new Recommendation(
                productId,
                productName(productId),
                null,
                null,
                null,
                null,
                null,
                List.of()
        );
    }

    private String productName(UUID productId) {
        return productRepository.findById(productId)
                .map(Producto::getNombre)
                .orElse(PRODUCT_NOT_FOUND);
    }

    private List<ItemListaPrecioProveedor> sortedByUnitPrice(
            List<ItemListaPrecioProveedor> items
    ) {
        return items.stream()
                .sorted(Comparator.comparing(this::unitPrice))
                .toList();
    }

    private List<Alternative> alternatives(
            List<ItemListaPrecioProveedor> sortedItems,
            SupplierIndex supplierIndex
    ) {
        return sortedItems.stream()
                .skip(1)
                .map(item -> alternative(item, supplierIndex))
                .toList();
    }

    private Alternative alternative(
            ItemListaPrecioProveedor item,
            SupplierIndex supplierIndex
    ) {
        UUID supplierId = supplierIndex.supplierIdFor(item.getListaPrecioId());
        return new Alternative(
                supplierId,
                supplierIndex.nameFor(supplierId),
                item.getPrecio(),
                item.getUnidad(),
                unitPrice(item)
        );
    }

    private BigDecimal unitPrice(ItemListaPrecioProveedor item) {
        if (item.getPrecio() == null) {
            return BigDecimal.ZERO;
        }

        int quantity = quantityParser.quantityFrom(item.getUnidad());
        return item.getPrecio().divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
    }

    private record SupplierIndex(
            Map<UUID, String> supplierNames,
            Map<UUID, UUID> priceListSupplierIds
    ) {
        UUID supplierIdFor(UUID priceListId) {
            return priceListSupplierIds.get(priceListId);
        }

        String nameFor(UUID supplierId) {
            return supplierId == null
                    ? UNKNOWN_SUPPLIER
                    : supplierNames.getOrDefault(supplierId, UNKNOWN_SUPPLIER);
        }
    }
}
