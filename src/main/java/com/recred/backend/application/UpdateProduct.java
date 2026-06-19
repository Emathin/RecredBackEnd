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
    public Producto execute(UpdateProductCommand command) {
        Producto existing = productRepository.findActiveById(command.id())
                .orElseThrow(() -> new ProductNotFoundException(command.id()));

        applyChanges(existing, command);
        Producto saved = productRepository.save(existing);

        if (hasInventoryData(command)) {
            updateInventory(command, saved.getId());
        }

        return saved;
    }

    private void applyChanges(Producto product, UpdateProductCommand command) {
        product.setNombre(command.nombre());
        product.setDescripcion(command.descripcion());
        product.setPrecio(command.precio());
        product.setPeso(command.peso());
        product.setRequierePreparacion(command.requierePreparacion());
        product.setUrlImagen(command.urlImagen());

        handleCategory(command, product);
        handleClasificacionesSalud(command, product);
        handleTipos(command, product);
    }

    private void handleCategory(UpdateProductCommand command, Producto product) {
        if (hasNewCategoryName(command)) {
            CategoriaProducto category = categoryRepository
                    .findByDescription(command.nuevaCategoriaNombre())
                    .orElseGet(() -> createAndSaveCategory(command.nuevaCategoriaNombre()));
            product.setCategoria(category);
            return;
        }

        if (command.categoriaId() != null) {
            CategoriaProducto categoria = new CategoriaProducto();
            categoria.setId(command.categoriaId());
            product.setCategoria(categoria);
        }
    }

    private boolean hasNewCategoryName(UpdateProductCommand command) {
        return command.nuevaCategoriaNombre() != null
                && !command.nuevaCategoriaNombre().isBlank();
    }

    private CategoriaProducto createAndSaveCategory(String name) {
        CategoriaProducto category = new CategoriaProducto();
        category.setDescripcion(name);
        category.setActivo(true);
        return categoryRepository.save(category);
    }

    private void handleClasificacionesSalud(UpdateProductCommand command, Producto product) {
        if (command.clasificacionesSaludIds() == null) {
            return;
        }

        Set<ClasificacionSalud> clasificaciones = command.clasificacionesSaludIds()
                .stream().map(id -> {
                    ClasificacionSalud cs = new ClasificacionSalud();
                    cs.setId(id);
                    return cs;
                }).collect(Collectors.toSet());
        product.setClasificacionesSalud(clasificaciones);
    }

    private void handleTipos(UpdateProductCommand command, Producto product) {
        if (command.tiposIds() == null) {
            return;
        }

        Set<ClasificacionTipo> tipos = command.tiposIds().stream().map(id -> {
            ClasificacionTipo ct = new ClasificacionTipo();
            ct.setId(id);
            return ct;
        }).collect(Collectors.toSet());
        product.setTipos(tipos);
    }

    private boolean hasInventoryData(UpdateProductCommand command) {
        return command.buffetId() != null
                && (command.stockActual() != null
                || command.tipoManejoInventario() != null
                || command.cupoMaximoDiario() != null);
    }

    private void updateInventory(UpdateProductCommand command, UUID productId) {
        Inventario inventario = inventoryRepository
                .findByBuffetAndProduct(command.buffetId(), productId)
                .orElseGet(() -> createNewInventory(command, productId));

        if (command.stockActual() != null) {
            inventario.setStockActual(command.stockActual());
        }
        inventario.setTipoManejo(tipoManejo(command));
        inventario.setCupoMaximoDiario(command.cupoMaximoDiario());
        inventario.setUrlImagen(command.urlImagen());
        inventoryRepository.save(inventario);
    }

    private Inventario createNewInventory(UpdateProductCommand command, UUID productId) {
        Inventario inventario = new Inventario();
        inventario.setBuffetId(command.buffetId());
        inventario.setProductoId(productId);
        inventario.setStockMinimo(Inventario.STOCK_MINIMO_PREDETERMINADO);
        inventario.setEstado(EstadoInventario.DISPONIBLE);
        inventario.setStockReservado(0);
        inventario.setTipoManejo(tipoManejo(command));
        return inventario;
    }

    private TipoManejoInventario tipoManejo(UpdateProductCommand command) {
        return command.tipoManejoInventario() == null
                ? TipoManejoInventario.STOCK_EXACTO
                : command.tipoManejoInventario();
    }
}
