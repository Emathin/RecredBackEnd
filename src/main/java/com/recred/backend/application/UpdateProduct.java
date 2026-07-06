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

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class UpdateProduct {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CategoryRepository categoryRepository;

    public UpdateProduct(ProductRepository productRepository,
                         InventoryRepository inventoryRepository,
                         CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public ProductResponse execute(UpdateProductRequest request) {
        Producto existing = productRepository.findActiveById(request.id())
                .orElseThrow(() -> new ProductNotFoundException(request.id()));

        applyChanges(existing, request);
        Producto saved = productRepository.save(existing);

        if (hasInventoryData(request)) {
            updateInventory(request, saved.getId());
        }

        return ProductResponse.fromDomain(saved);
    }

    private void applyChanges(Producto product, UpdateProductRequest request) {
        product.setNombre(request.nombre());
        product.setDescripcion(request.descripcion());
        product.setPrecio(request.precio());
        product.setPeso(request.peso());
        product.setRequierePreparacion(request.requierePreparacion());
        product.setUrlImagen(request.urlImagen());

        handleCategory(request, product);
        handleClasificacionesSalud(request, product);
        handleTipos(request, product);
    }

    private void handleCategory(UpdateProductRequest request, Producto product) {
        if (hasNewCategoryName(request)) {
            CategoriaProducto category = categoryRepository
                    .findByDescription(request.nuevaCategoriaNombre())
                    .orElseGet(() -> createAndSaveCategory(request.nuevaCategoriaNombre()));
            product.setCategoria(category);
            return;
        }

        if (request.categoriaId() != null) {
            CategoriaProducto categoria = new CategoriaProducto();
            categoria.setId(request.categoriaId());
            product.setCategoria(categoria);
        }
    }

    private boolean hasNewCategoryName(UpdateProductRequest request) {
        return request.nuevaCategoriaNombre() != null
                && !request.nuevaCategoriaNombre().isBlank();
    }

    private CategoriaProducto createAndSaveCategory(String name) {
        CategoriaProducto category = new CategoriaProducto();
        category.setDescripcion(name);
        category.setActivo(true);
        return categoryRepository.save(category);
    }

    private void handleClasificacionesSalud(UpdateProductRequest request, Producto product) {
        if (request.clasificacionesSaludIds() == null) {
            return;
        }

        Set<ClasificacionSalud> clasificaciones = request.clasificacionesSaludIds()
                .stream().map(id -> {
                    ClasificacionSalud cs = new ClasificacionSalud();
                    cs.setId(id);
                    return cs;
                }).collect(Collectors.toSet());
        product.setClasificacionesSalud(clasificaciones);
    }

    private void handleTipos(UpdateProductRequest request, Producto product) {
        if (request.tiposIds() == null) {
            return;
        }

        Set<ClasificacionTipo> tipos = request.tiposIds().stream().map(id -> {
            ClasificacionTipo ct = new ClasificacionTipo();
            ct.setId(id);
            return ct;
        }).collect(Collectors.toSet());
        product.setTipos(tipos);
    }

    private boolean hasInventoryData(UpdateProductRequest request) {
        return request.buffetId() != null
                && (request.stockActual() != null
                || request.tipoManejoInventario() != null
                || request.cupoMaximoDiario() != null);
    }

    private void updateInventory(UpdateProductRequest request, UUID productId) {
        Inventario inventario = inventoryRepository
                .findByBuffetAndProduct(request.buffetId(), productId)
                .orElseGet(() -> createNewInventory(request, productId));

        if (request.stockActual() != null) {
            inventario.setStockActual(request.stockActual());
        }
        inventario.setTipoManejo(tipoManejo(request));
        inventario.setCupoMaximoDiario(request.cupoMaximoDiario());
        inventario.setUrlImagen(request.urlImagen());
        inventoryRepository.save(inventario);
    }

    private Inventario createNewInventory(UpdateProductRequest request, UUID productId) {
        Inventario inventario = new Inventario();
        inventario.setBuffetId(request.buffetId());
        inventario.setProductoId(productId);
        inventario.setStockMinimo(Inventario.STOCK_MINIMO_PREDETERMINADO);
        inventario.setEstado(EstadoInventario.DISPONIBLE);
        inventario.setStockReservado(0);
        inventario.setTipoManejo(tipoManejo(request));
        return inventario;
    }

    private TipoManejoInventario tipoManejo(UpdateProductRequest request) {
        return request.tipoManejoInventario() == null
                ? TipoManejoInventario.STOCK_EXACTO
                : request.tipoManejoInventario();
    }
}
