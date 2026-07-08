package com.recred.backend.application;

import com.recred.backend.domain.exceptions.ProductNotFoundException;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.ClasificacionTipo;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.repositories.CategoryRepository;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

public class UpdateProduct {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CategoryRepository categoryRepository;

    public UpdateProduct(
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            CategoryRepository categoryRepository
    ) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public ProductResponse execute(UpdateProductRequest productRequest) {
        Producto product = findActiveProduct(productRequest.id());
        updateProductDetails(product, productRequest);
        Producto savedProduct = productRepository.save(product);

        if (requestHasInventoryChanges(productRequest)) {
            updateInventoryFor(productRequest, savedProduct.getId());
        }

        return ProductResponse.fromDomain(savedProduct);
    }

    private Producto findActiveProduct(UUID productId) {
        return productRepository.findActiveById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private void updateProductDetails(Producto product, UpdateProductRequest productRequest) {
        product.setNombre(productRequest.nombre());
        product.setDescripcion(productRequest.descripcion());
        product.setPrecio(productRequest.precio());
        product.setPeso(productRequest.peso());
        product.setRequierePreparacion(productRequest.requierePreparacion());
        product.setUrlImagen(productRequest.urlImagen());

        updateCategoryIfRequested(productRequest, product);
        updateHealthClassificationsIfRequested(productRequest, product);
        updateTypesIfRequested(productRequest, product);
    }

    private void updateCategoryIfRequested(UpdateProductRequest productRequest, Producto product) {
        if (requestHasNewCategoryName(productRequest)) {
            CategoriaProducto category = categoryRepository
                    .findByDescription(productRequest.nuevaCategoriaNombre())
                    .orElseGet(() -> createAndSaveCategory(
                            productRequest.nuevaCategoriaNombre()));
            product.setCategoria(category);
            return;
        }

        if (productRequest.categoriaId() != null) {
            product.setCategoria(categoryReference(productRequest.categoriaId()));
        }
    }

    private boolean requestHasNewCategoryName(UpdateProductRequest productRequest) {
        return productRequest.nuevaCategoriaNombre() != null
                && !productRequest.nuevaCategoriaNombre().isBlank();
    }

    private CategoriaProducto createAndSaveCategory(String name) {
        CategoriaProducto category = new CategoriaProducto();
        category.setDescripcion(name);
        category.setActivo(true);
        return categoryRepository.save(category);
    }

    private CategoriaProducto categoryReference(UUID categoryId) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(categoryId);
        return category;
    }

    private void updateHealthClassificationsIfRequested(
            UpdateProductRequest productRequest,
            Producto product
    ) {
        if (productRequest.clasificacionesSaludIds() == null) {
            return;
        }

        product.setClasificacionesSalud(productRequest.clasificacionesSaludIds()
                .stream()
                .map(this::healthClassificationReference)
                .collect(Collectors.toSet()));
    }

    private ClasificacionSalud healthClassificationReference(UUID healthClassificationId) {
        ClasificacionSalud healthClassification = new ClasificacionSalud();
        healthClassification.setId(healthClassificationId);
        return healthClassification;
    }

    private void updateTypesIfRequested(UpdateProductRequest productRequest, Producto product) {
        if (productRequest.tiposIds() == null) {
            return;
        }

        product.setTipos(productRequest.tiposIds()
                .stream()
                .map(this::typeReference)
                .collect(Collectors.toSet()));
    }

    private ClasificacionTipo typeReference(UUID typeId) {
        ClasificacionTipo type = new ClasificacionTipo();
        type.setId(typeId);
        return type;
    }

    private boolean requestHasInventoryChanges(UpdateProductRequest productRequest) {
        return productRequest.buffetId() != null
                && (productRequest.stockActual() != null
                || productRequest.tipoManejoInventario() != null
                || productRequest.cupoMaximoDiario() != null);
    }

    private void updateInventoryFor(UpdateProductRequest productRequest, UUID productId) {
        Inventario inventario = inventoryRepository
                .findByBuffetAndProduct(productRequest.buffetId(), productId)
                .orElseGet(() -> newInventoryFor(productRequest, productId));

        if (productRequest.stockActual() != null) {
            inventario.setStockActual(productRequest.stockActual());
        }
        inventario.setTipoManejo(inventoryHandlingTypeFrom(productRequest));
        inventario.setCupoMaximoDiario(productRequest.cupoMaximoDiario());
        inventario.setUrlImagen(productRequest.urlImagen());
        inventoryRepository.save(inventario);
    }

    private Inventario newInventoryFor(UpdateProductRequest productRequest, UUID productId) {
        Inventario inventario = new Inventario();
        inventario.setBuffetId(productRequest.buffetId());
        inventario.setProductoId(productId);
        inventario.setStockMinimo(Inventario.STOCK_MINIMO_PREDETERMINADO);
        inventario.setEstado(EstadoInventario.DISPONIBLE);
        inventario.setStockReservado(0);
        inventario.setTipoManejo(inventoryHandlingTypeFrom(productRequest));
        return inventario;
    }

    private TipoManejoInventario inventoryHandlingTypeFrom(UpdateProductRequest productRequest) {
        return productRequest.tipoManejoInventario() == null
                ? TipoManejoInventario.STOCK_EXACTO
                : productRequest.tipoManejoInventario();
    }
}
