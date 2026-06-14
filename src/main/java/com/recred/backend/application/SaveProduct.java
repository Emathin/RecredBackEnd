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
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Producto execute(SaveProductCommand command) {
        Producto producto = mapToModel(command);
        Producto savedProduct = productRepository.save(producto);

        if (hasInventoryData(command)) {
            saveInventory(command, savedProduct);
            savedProduct.setStockActual(command.stockActual());
            savedProduct.setStockReservado(0);
            savedProduct.setStockDisponible(command.stockActual() == null
                    ? null
                    : command.stockActual());
            savedProduct.setTipoManejoInventario(tipoManejo(command));
            savedProduct.setCupoMaximoDiario(command.cupoMaximoDiario());
        }

        return savedProduct;
    }

    private void saveInventory(SaveProductCommand command, Producto savedProduct) {
        Inventario inventario = inventoryRepository
                .findByBuffetAndProduct(command.buffetId(), savedProduct.getId())
                .orElseGet(() -> createNewInventory(command, savedProduct.getId()));

        if (command.stockActual() != null) {
            inventario.setStockActual(command.stockActual());
        }
        inventario.setTipoManejo(tipoManejo(command));
        inventario.setCupoMaximoDiario(command.cupoMaximoDiario());
        inventoryRepository.save(inventario);
    }

    private boolean hasInventoryData(SaveProductCommand command) {
        return command.buffetId() != null
                && (command.stockActual() != null
                || command.tipoManejoInventario() != null
                || command.cupoMaximoDiario() != null);
    }

    private Inventario createNewInventory(SaveProductCommand command, UUID productoId) {
        Inventario inventario = new Inventario();
        inventario.setBuffetId(command.buffetId());
        inventario.setProductoId(productoId);
        inventario.setStockMinimo(Inventario.STOCK_MINIMO_PREDETERMINADO);
        inventario.setEstado(EstadoInventario.DISPONIBLE);
        inventario.setStockReservado(0);
        inventario.setTipoManejo(tipoManejo(command));
        return inventario;
    }

    private TipoManejoInventario tipoManejo(SaveProductCommand command) {
        return command.tipoManejoInventario() == null
                ? TipoManejoInventario.STOCK_EXACTO
                : command.tipoManejoInventario();
    }

    private Producto mapToModel(SaveProductCommand command) {
        Producto producto = new Producto();
        producto.setNombre(command.nombre());
        producto.setDescripcion(command.descripcion());
        producto.setPrecio(command.precio());
        producto.setPeso(command.peso());
        producto.setRequierePreparacion(command.requierePreparacion());
        producto.setActivo(true);

        handleCategory(command, producto);

        if (command.clasificacionesSaludIds() != null) {
            Set<ClasificacionSalud> clasificaciones = command.clasificacionesSaludIds()
                    .stream().map(id -> {
                        ClasificacionSalud cs = new ClasificacionSalud();
                        cs.setId(id);
                        return cs;
                    }).collect(Collectors.toSet());
            producto.setClasificacionesSalud(clasificaciones);
        }

        if (command.tiposIds() != null) {
            Set<ClasificacionTipo> tipos = command.tiposIds().stream().map(id -> {
                ClasificacionTipo ct = new ClasificacionTipo();
                ct.setId(id);
                return ct;
            }).collect(Collectors.toSet());
            producto.setTipos(tipos);
        }

        return producto;
    }

    private void handleCategory(SaveProductCommand command, Producto producto) {
        if (command.nuevaCategoriaNombre() != null
                && !command.nuevaCategoriaNombre().isBlank()) {
            CategoriaProducto category = categoryRepository
                    .findByDescription(command.nuevaCategoriaNombre())
                    .orElseGet(() -> createAndSaveCategory(command.nuevaCategoriaNombre()));
            producto.setCategoria(category);
            return;
        }

        if (command.categoriaId() != null) {
            CategoriaProducto categoria = new CategoriaProducto();
            categoria.setId(command.categoriaId());
            producto.setCategoria(categoria);
        }
    }

    private CategoriaProducto createAndSaveCategory(String name) {
        CategoriaProducto category = new CategoriaProducto();
        category.setDescripcion(name);
        category.setActivo(true);
        return categoryRepository.save(category);
    }
}
