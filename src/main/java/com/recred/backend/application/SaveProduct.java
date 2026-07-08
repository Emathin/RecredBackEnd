package com.recred.backend.application;

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

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SaveProduct {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CategoryRepository categoryRepository;

    public SaveProduct(ProductRepository productRepository,
                       InventoryRepository inventoryRepository,
                       CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.categoryRepository = categoryRepository;
    }

    public ProductResponse execute(SaveProductRequest request) {
        Producto producto = mapToModel(request);
        Producto savedProduct = persistProduct(producto);

        if (hasInventoryData(request)) {
            saveInventory(request, savedProduct);
            savedProduct.setStockActual(request.stockActual());
            savedProduct.setStockReservado(0);
            savedProduct.setStockDisponible(request.stockActual() == null
                    ? null
                    : request.stockActual());
            savedProduct.setTipoManejoInventario(tipoManejo(request));
            savedProduct.setCupoMaximoDiario(request.cupoMaximoDiario());
        }

        return ProductResponse.fromDomain(savedProduct);
    }

    private void saveInventory(SaveProductRequest request, Producto savedProduct) {
        Inventario inventario = findInventoryByBuffetAndProduct(
                request.buffetId(), savedProduct.getId())
                .orElseGet(() -> createNewInventory(request, savedProduct.getId()));

        if (request.stockActual() != null) {
            inventario.setStockActual(request.stockActual());
        }
        inventario.setTipoManejo(tipoManejo(request));
        inventario.setCupoMaximoDiario(request.cupoMaximoDiario());
        inventario.setUrlImagen(request.urlImagen());
        persistInventory(inventario);
    }

    private boolean hasInventoryData(SaveProductRequest request) {
        return request.buffetId() != null
                && (request.stockActual() != null
                || request.tipoManejoInventario() != null
                || request.cupoMaximoDiario() != null);
    }

    private Inventario createNewInventory(SaveProductRequest request, UUID productoId) {
        Inventario inventario = new Inventario();
        inventario.setBuffetId(request.buffetId());
        inventario.setProductoId(productoId);
        inventario.setStockMinimo(Inventario.STOCK_MINIMO_PREDETERMINADO);
        inventario.setEstado(EstadoInventario.DISPONIBLE);
        inventario.setStockReservado(0);
        inventario.setTipoManejo(tipoManejo(request));
        return inventario;
    }

    private TipoManejoInventario tipoManejo(SaveProductRequest request) {
        return request.tipoManejoInventario() == null
                ? TipoManejoInventario.STOCK_EXACTO
                : request.tipoManejoInventario();
    }

    private Producto mapToModel(SaveProductRequest request) {
        Producto producto = new Producto();
        producto.setNombre(request.nombre());
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precio());
        producto.setPeso(request.peso());
        producto.setRequierePreparacion(request.requierePreparacion());
        producto.setUrlImagen(request.urlImagen());
        producto.setActivo(true);

        handleCategory(request, producto);

        if (request.clasificacionesSaludIds() != null) {
            Set<ClasificacionSalud> clasificaciones = request.clasificacionesSaludIds()
                    .stream().map(id -> {
                        ClasificacionSalud cs = new ClasificacionSalud();
                        cs.setId(id);
                        return cs;
                    }).collect(Collectors.toSet());
            producto.setClasificacionesSalud(clasificaciones);
        }

        if (request.tiposIds() != null) {
            Set<ClasificacionTipo> tipos = request.tiposIds().stream().map(id -> {
                ClasificacionTipo ct = new ClasificacionTipo();
                ct.setId(id);
                return ct;
            }).collect(Collectors.toSet());
            producto.setTipos(tipos);
        }

        return producto;
    }

    private void handleCategory(SaveProductRequest request, Producto producto) {
        if (request.nuevaCategoriaNombre() != null
                && !request.nuevaCategoriaNombre().isBlank()) {
            CategoriaProducto category = findCategoryByDescription(request.nuevaCategoriaNombre())
                    .orElseGet(() -> createAndSaveCategory(request.nuevaCategoriaNombre()));
            producto.setCategoria(category);
            return;
        }

        if (request.categoriaId() != null) {
            CategoriaProducto categoria = new CategoriaProducto();
            categoria.setId(request.categoriaId());
            producto.setCategoria(categoria);
        }
    }

    private CategoriaProducto createAndSaveCategory(String name) {
        CategoriaProducto category = new CategoriaProducto();
        category.setDescripcion(name);
        category.setActivo(true);
        return persistCategory(category);
    }

    private Producto persistProduct(Producto producto) {
        return productRepository.save(producto);
    }

    private void persistInventory(Inventario inventario) {
        inventoryRepository.save(inventario);
    }

    private java.util.Optional<Inventario> findInventoryByBuffetAndProduct(
            UUID buffetId, UUID productId) {
        return inventoryRepository.findByBuffetAndProduct(buffetId, productId);
    }

    private CategoriaProducto persistCategory(CategoriaProducto category) {
        return categoryRepository.save(category);
    }

    private java.util.Optional<CategoriaProducto> findCategoryByDescription(String description) {
        return categoryRepository.findByDescription(description);
    }
}
