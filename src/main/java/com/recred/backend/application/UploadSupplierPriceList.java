package com.recred.backend.application;

import com.recred.backend.domain.exceptions.OperationNotAllowedException;
import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import com.recred.backend.domain.models.ListaPrecioProveedor;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ItemListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.ListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.ProveedorRepository;
import com.recred.backend.domain.services.StorageService;
import com.recred.backend.domain.services.SupplierPriceListAnalyzer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class UploadSupplierPriceList {

    private final ProveedorRepository supplierRepository;
    private final ListaPrecioProveedorRepository priceListRepository;
    private final ItemListaPrecioProveedorRepository priceListItemRepository;
    private final ProductRepository productRepository;
    private final StorageService storageService;
    private final SupplierPriceListAnalyzer supplierPriceListAnalyzer;
    private final SupplierProductAutoMatcher autoMatcher;

    public UploadSupplierPriceList(
            ProveedorRepository supplierRepository,
            ListaPrecioProveedorRepository priceListRepository,
            ItemListaPrecioProveedorRepository priceListItemRepository,
            ProductRepository productRepository,
            StorageService storageService,
            SupplierPriceListAnalyzer supplierPriceListAnalyzer
    ) {
        this.supplierRepository = supplierRepository;
        this.priceListRepository = priceListRepository;
        this.priceListItemRepository = priceListItemRepository;
        this.productRepository = productRepository;
        this.storageService = storageService;
        this.supplierPriceListAnalyzer = supplierPriceListAnalyzer;
        this.autoMatcher = new SupplierProductAutoMatcher();
    }

    public UploadSupplierPriceListResponse execute(UploadSupplierPriceListRequest request) {
        validateRequestAccess(request);
        ListaPrecioProveedor priceList = createPriceListWithItems(request);

        return UploadSupplierPriceListResponse.fromDomain(priceList);
    }

    private Proveedor findSupplier(UUID supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
    }

    private void requireSupplierOwnership(Proveedor supplier, UUID kioskOwnerId) {
        if (!supplier.belongsToKiosquero(kioskOwnerId)) {
            throw new OperationNotAllowedException("You do not have access to this supplier");
        }
    }

    private void validateRequestAccess(UploadSupplierPriceListRequest request) {
        request.validate();
        Proveedor supplier = findSupplier(request.supplierId());
        requireSupplierOwnership(supplier, request.kioskOwnerId());
    }

    private ListaPrecioProveedor createPriceListWithItems(UploadSupplierPriceListRequest request) {
        String fileUrl = uploadFile(request);
        List<RawSupplierPriceItem> rawItems = analyzePriceList(request);
        List<Producto> activeProducts = findActiveProducts();
        ListaPrecioProveedor priceList = savePriceList(request, fileUrl);
        priceList.setItems(saveItems(priceList.getId(), rawItems, activeProducts));
        return priceList;
    }

    private String uploadFile(UploadSupplierPriceListRequest request) {
        return storageService.uploadFile(
                request.fileContent(),
                request.originalFilename(),
                request.resolvedMimeType()
        );
    }

    private List<RawSupplierPriceItem> analyzePriceList(UploadSupplierPriceListRequest request) {
        return supplierPriceListAnalyzer.analyze(
                request.fileContent(),
                request.resolvedMimeType()
        );
    }

    private List<Producto> findActiveProducts() {
        return productRepository.findAllActive();
    }

    private ListaPrecioProveedor savePriceList(
            UploadSupplierPriceListRequest request,
            String fileUrl
    ) {
        ListaPrecioProveedor priceList = new ListaPrecioProveedor();
        priceList.setProveedorId(request.supplierId());
        priceList.setUrlArchivo(fileUrl);
        priceList.setNombreOriginal(request.originalFilename());
        priceList.setActiva(true);
        priceList.setCreadoEn(LocalDateTime.now());
        return priceListRepository.save(priceList);
    }

    private List<ItemListaPrecioProveedor> saveItems(
            UUID priceListId,
            List<RawSupplierPriceItem> rawItems,
            List<Producto> activeProducts
    ) {
        return priceListItemRepository.saveAll(
                rawItems.stream()
                        .map(rawItem -> priceItem(priceListId, rawItem, activeProducts))
                        .toList()
        );
    }

    private ItemListaPrecioProveedor priceItem(
            UUID priceListId,
            RawSupplierPriceItem rawItem,
            List<Producto> activeProducts
    ) {
        ItemListaPrecioProveedor item = new ItemListaPrecioProveedor();
        item.setListaPrecioId(priceListId);
        item.setNombreProductoProveedor(rawItem.nombreProducto());
        item.setPrecio(rawItem.precio());
        item.setUnidad(rawItem.unidad());
        item.setNotas(rawItem.notas());
        item.setMappingConfirmado(false);
        applyAutoMatch(item, activeProducts);
        return item;
    }

    private void applyAutoMatch(
            ItemListaPrecioProveedor item,
            List<Producto> activeProducts
    ) {
        autoMatcher.match(item.getNombreProductoProveedor(), activeProducts)
                .ifPresent(product -> {
                    item.setProductoInventarioId(product.getId());
                    item.setNombreProductoInventario(product.getNombre());
                });
    }
}
