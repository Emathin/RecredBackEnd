package com.recred.backend.integration.persistence;

import com.recred.backend.domain.exceptions.InventoryOperationException;
import com.recred.backend.domain.models.*;
import com.recred.backend.infrastructure.entities.*;
import com.recred.backend.infrastructure.persistence.ProductRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataProductRepository;
import org.junit.jupiter.api.BeforeEach;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.persistence.ProductStockResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductRepositoryImplTest {
    @Mock
    private SpringDataProductRepository springDataRepository;

    @InjectMocks
    private ProductRepositoryImpl repositoryImpl;
    private ProductRepositoryImpl productRepository;

    @BeforeEach
    void setUp() {
        productRepository = new ProductRepositoryImpl(springDataRepository);
    }

    @Test
    void findByIdReturnsDomainObjectIfExists() {
        UUID id = UUID.randomUUID();
        ProductoEntity entity = new ProductoEntity();
        entity.setId(id);
        entity.setNombre("Producto Test");

        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<Producto> result = repositoryImpl.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        assertEquals("Producto Test", result.get().getNombre());
    }

    @Test
    void findById_DeberiaRetornarVacio_CuandoNoExiste() {
        UUID id = UUID.randomUUID();
        when(springDataRepository.findById(id)).thenReturn(Optional.empty());

        Optional<Producto> result = productRepository.findById(id);

        assertTrue(result.isEmpty());
    }

    @Test
    void save_DeberiaCrearNuevaEntidad_CuandoProductoNoTieneId() {
        Producto producto = new Producto();
        producto.setNombre("Nuevo");
        CategoriaProducto cat = new CategoriaProducto();
        cat.setId(UUID.randomUUID());
        producto.setCategoria(cat);

        ProductoEntity savedEntity = new ProductoEntity();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setNombre("Nuevo");
        when(springDataRepository.save(any(ProductoEntity.class))).thenReturn(savedEntity);

        Producto result = productRepository.save(producto);

        assertNotNull(result.getId());
        verify(springDataRepository).save(any(ProductoEntity.class));
    }

    @Test
    void save_DeberiaActualizarEntidadExistente_CuandoProductoTieneId() {
        UUID id = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre("Actualizado");
        CategoriaProducto cat = new CategoriaProducto();
        cat.setId(UUID.randomUUID());
        producto.setCategoria(cat);

        ProductoEntity existingEntity = new ProductoEntity();
        existingEntity.setId(id);
        existingEntity.setNombre("Viejo");

        when(springDataRepository.findById(id)).thenReturn(Optional.of(existingEntity));
        when(springDataRepository.save(any(ProductoEntity.class))).thenAnswer(i -> i.getArgument(0));

        productRepository.save(producto);

        ArgumentCaptor<ProductoEntity> captor = ArgumentCaptor.forClass(ProductoEntity.class);
        verify(springDataRepository).save(captor.capture());
        assertEquals("Actualizado", captor.getValue().getNombre());
    }

    @Test
    void save_DeberiaMapearTodo_IncluyendoMedidasTiposYClasificaciones() {
        Producto producto = new Producto();
        producto.setNombre("Completo");

        CategoriaProducto cat = new CategoriaProducto();
        cat.setId(UUID.randomUUID());
        producto.setCategoria(cat);

        ProductoMedida medida = new ProductoMedida();
        medida.setId(UUID.randomUUID());
        producto.addMedida(medida);

        ClasificacionTipo tipo = new ClasificacionTipo();
        tipo.setId(UUID.randomUUID());
        producto.setTipos(Set.of(tipo));

        ClasificacionSalud salud = new ClasificacionSalud();
        salud.setId(UUID.randomUUID());
        producto.setClasificacionesSalud(Set.of(salud));

        when(springDataRepository.save(any(ProductoEntity.class))).thenAnswer(i -> i.getArgument(0));

        productRepository.save(producto);

        ArgumentCaptor<ProductoEntity> captor = ArgumentCaptor.forClass(ProductoEntity.class);
        verify(springDataRepository).save(captor.capture());
        ProductoEntity saved = captor.getValue();

        assertNotNull(saved.getCategoria());
        assertFalse(saved.getMedidas().isEmpty());
        assertFalse(saved.getTipos().isEmpty());
        assertFalse(saved.getClasificacionesSalud().isEmpty());
    }

    @Test
    void save_DeberiaLimpiarColecciones_CuandoVienenVacias() {
        UUID id = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(id);
        CategoriaProducto cat = new CategoriaProducto();
        cat.setId(UUID.randomUUID());
        producto.setCategoria(cat);

        ProductoEntity entity = new ProductoEntity();
        entity.setId(id);
        entity.getMedidas().add(new ProductoMedidaEntity());
        entity.getTipos().add(new ClasificacionTipoEntity());
        entity.getClasificacionesSalud().add(new ClasificacionSaludEntity());

        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));
        when(springDataRepository.save(any(ProductoEntity.class))).thenAnswer(i -> i.getArgument(0));

        productRepository.save(producto);

        assertTrue(entity.getMedidas().isEmpty());
        assertTrue(entity.getTipos().isEmpty());
        assertTrue(entity.getClasificacionesSalud().isEmpty());
    }

    @Test
    void findBuffetLeastSoldProductsReturnsAProduct() {
        UUID buffetId = java.util.UUID.randomUUID();
        ProductoEntity entity = new ProductoEntity();
        entity.setId(java.util.UUID.randomUUID());
        entity.setNombre("Menos Vendido");
        entity.setPrecio(new java.math.BigDecimal("100.00"));

        when(springDataRepository.findBuffetLeastSoldProducts(buffetId)).thenReturn(entity);

        ProductChangeRecommendation result = repositoryImpl.findBuffetLeastSoldProducts(buffetId);

        assertNotNull(result);
        assertEquals(entity.getId(), result.getId());
        assertEquals(buffetId, result.getIdBuffet());
    }
    @Test
    void save_DeberiaNoReasignarCategoria_CuandoIdEsIgual() {
        UUID id = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(id);
        CategoriaProducto cat = new CategoriaProducto();
        cat.setId(catId);
        producto.setCategoria(cat);

        ProductoEntity entity = new ProductoEntity();
        entity.setId(id);
        CategoriaProductoEntity catEntity = new CategoriaProductoEntity();
        catEntity.setId(catId);
        entity.setCategoria(catEntity);

        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));
        when(springDataRepository.save(any(ProductoEntity.class))).thenAnswer(i -> i.getArgument(0));

        productRepository.save(producto);

        assertSame(catEntity, entity.getCategoria());
    }

    @Test
    void save_DeberiaLimpiarColecciones_CuandoVienenNulasEnModelo() {
        UUID id = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(id);
        producto.setMedidas(null);
        producto.setTipos(null);
        producto.setClasificacionesSalud(null);
        CategoriaProducto cat = new CategoriaProducto();
        cat.setId(UUID.randomUUID());
        producto.setCategoria(cat);

        ProductoEntity entity = new ProductoEntity();
        entity.setId(id);

        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));
        when(springDataRepository.save(any(ProductoEntity.class))).thenAnswer(i -> i.getArgument(0));

        productRepository.save(producto);

        verify(springDataRepository).save(any(ProductoEntity.class));
    }

    @Test
    void save_DeberiaLimpiarColecciones_CuandoVienenVaciasEnModelo() {
        UUID id = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(id);
        producto.setMedidas(new java.util.HashSet<>());
        producto.setTipos(new java.util.HashSet<>());
        producto.setClasificacionesSalud(new java.util.HashSet<>());
        CategoriaProducto cat = new CategoriaProducto();
        cat.setId(UUID.randomUUID());
        producto.setCategoria(cat);

        ProductoEntity entity = new ProductoEntity();
        entity.setId(id);

        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));
        when(springDataRepository.save(any(ProductoEntity.class))).thenAnswer(i -> i.getArgument(0));

        productRepository.save(producto);

        verify(springDataRepository).save(any(ProductoEntity.class));
    }

    @Test
    void save_DeberiaCrearNuevaEntidad_CuandoIdNoExisteEnBaseDeDatos() {
        UUID id = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre("Nuevo");
        CategoriaProducto cat = new CategoriaProducto();
        cat.setId(UUID.randomUUID());
        producto.setCategoria(cat);

        when(springDataRepository.findById(id)).thenReturn(Optional.empty());
        when(springDataRepository.save(any(ProductoEntity.class))).thenAnswer(i -> i.getArgument(0));

        productRepository.save(producto);

        verify(springDataRepository).save(any(ProductoEntity.class));
    }

    @Test
    void save_DeberiaCambiarCategoria_CuandoIdEsDiferente() {
        UUID id = UUID.randomUUID();
        UUID catId1 = UUID.randomUUID();
        UUID catId2 = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(id);
        CategoriaProducto cat = new CategoriaProducto();
        cat.setId(catId2);
        producto.setCategoria(cat);

        ProductoEntity entity = new ProductoEntity();
        entity.setId(id);
        CategoriaProductoEntity catEntity = new CategoriaProductoEntity();
        catEntity.setId(catId1);
        entity.setCategoria(catEntity);

        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));
        when(springDataRepository.save(any(ProductoEntity.class))).thenAnswer(i -> i.getArgument(0));

        productRepository.save(producto);

        assertEquals(catId2, entity.getCategoria().getId());
    }
    @Test
    void dadoQueSeLlamaFindAllActiveWithStock_cuandoExisteStock_entoncesMapeaCorrectamente() {
        UUID buffetId = UUID.randomUUID();
        ProductoEntity entity = new ProductoEntity();
        entity.setId(UUID.randomUUID());
        ProductStockResult resultObj = stockResult(
                entity,
                50,
                5,
                3,
                EstadoInventario.DISPONIBLE,
                TipoManejoInventario.STOCK_EXACTO,
                20,
                45
        );

        when(springDataRepository.findAllActiveWithStockParams(buffetId))
                .thenReturn(java.util.Collections.singletonList(resultObj));

        java.util.List<Producto> result = productRepository.findAllActiveWithStock(buffetId);

        assertFalse(result.isEmpty());
        assertEquals(50, result.get(0).getStockActual());
        assertEquals(5, result.get(0).getStockReservado());
        assertEquals(3, result.get(0).getStockMinimo());
        assertEquals(20, result.get(0).getCupoMaximoDiario());
        assertEquals(45, result.get(0).getStockDisponible());
    }

    @Test
    void findAllActiveWithStock_CuandoNoTieneMinimoYQuedanCuatro_DeberiaMapearBajoStock() {
        UUID buffetId = UUID.randomUUID();
        ProductoEntity entity = new ProductoEntity();
        entity.setId(UUID.randomUUID());
        ProductStockResult resultObj = stockResult(
                entity,
                4,
                0,
                null,
                EstadoInventario.DISPONIBLE,
                TipoManejoInventario.STOCK_EXACTO,
                null,
                4
        );

        when(springDataRepository.findAllActiveWithStockParams(buffetId))
                .thenReturn(java.util.Collections.singletonList(resultObj));

        java.util.List<Producto> result = productRepository.findAllActiveWithStock(buffetId);

        assertEquals(EstadoInventario.BAJO_STOCK, result.get(0).getEstadoInventario());
        assertEquals(Inventario.STOCK_MINIMO_PREDETERMINADO, result.get(0).getStockMinimo());
    }

    @Test
    void findAllActiveWithStock_CuandoNoTieneStock_DeberiaMapearSinStock() {
        UUID buffetId = UUID.randomUUID();
        ProductoEntity entity = new ProductoEntity();
        entity.setId(UUID.randomUUID());
        ProductStockResult resultObj = stockResult(
                entity,
                0,
                0,
                null,
                EstadoInventario.DISPONIBLE,
                TipoManejoInventario.STOCK_EXACTO,
                null,
                0
        );

        when(springDataRepository.findAllActiveWithStockParams(buffetId))
                .thenReturn(java.util.Collections.singletonList(resultObj));

        java.util.List<Producto> result = productRepository.findAllActiveWithStock(buffetId);

        assertEquals(EstadoInventario.SIN_STOCK, result.get(0).getEstadoInventario());
    }

    @Test
    void findActiveByIdWithStock_WhenThereIsNoInventoryData_ShouldReturnProductWithoutStock() {
        UUID id = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        ProductoEntity entity = new ProductoEntity();
        entity.setId(id);
        entity.setNombre("Sin inventario");
        ProductStockResult resultObj = stockResult(
                entity,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(springDataRepository.findActiveByIdWithStockParams(id, buffetId))
                .thenReturn(Optional.of(resultObj));

        Optional<Producto> result = productRepository.findActiveByIdWithStock(id, buffetId);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        assertNull(result.get().getStockActual());
        assertNull(result.get().getEstadoInventario());
    }

    @Test
    void dadoQueSeLlamaFindActiveByIdWithStock_cuandoExiste_entoncesMapeaCorrectamente() {
        UUID id = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        ProductoEntity entity = new ProductoEntity();
        entity.setId(id);
        ProductStockResult resultObj = stockResult(
                entity,
                10,
                2,
                1,
                EstadoInventario.DISPONIBLE,
                TipoManejoInventario.STOCK_EXACTO,
                null,
                8
        );

        when(springDataRepository.findActiveByIdWithStockParams(id, buffetId))
                .thenReturn(Optional.of(resultObj));

        Optional<Producto> result = productRepository.findActiveByIdWithStock(id, buffetId);

        assertTrue(result.isPresent());
        assertEquals(10, result.get().getStockActual());
        assertEquals(8, result.get().getStockDisponible());
    }

    @Test
    void findAllActiveReturnsMappedDomains() {
        ProductoEntity entity = new ProductoEntity();
        entity.setId(UUID.randomUUID());
        entity.setNombre("Activo");
        when(springDataRepository.findAllByActivoTrue()).thenReturn(List.of(entity));

        List<Producto> result = productRepository.findAllActive();

        assertEquals(1, result.size());
        assertEquals("Activo", result.get(0).getNombre());
    }

    @Test
    void findActiveByIdReturnsMappedDomain() {
        UUID id = UUID.randomUUID();
        ProductoEntity entity = new ProductoEntity();
        entity.setId(id);
        entity.setNombre("Activo");
        when(springDataRepository.findByIdAndActivoTrue(id)).thenReturn(Optional.of(entity));

        Optional<Producto> result = productRepository.findActiveById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.orElseThrow().getId());
    }

    @Test
    void findActiveByIdWithStockReturnsEmptyWhenSpringDataReturnsEmpty() {
        UUID id = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        when(springDataRepository.findActiveByIdWithStockParams(id, buffetId))
                .thenReturn(Optional.empty());

        Optional<Producto> result = productRepository.findActiveByIdWithStock(id, buffetId);

        assertTrue(result.isEmpty());
    }

    @Test
    void findAllActiveWithStockWhenProductDataIsInvalidThrowsControlledException() {
        UUID buffetId = UUID.randomUUID();
        ProductStockResult resultObj = stockResult(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        when(springDataRepository.findAllActiveWithStockParams(buffetId))
                .thenReturn(List.of(resultObj));

        assertThrows(
                InventoryOperationException.class,
                () -> productRepository.findAllActiveWithStock(buffetId)
        );
    }

    @Test
    void findBuffetTop5LeastSoldProductsMapsRecommendations() {
        UUID buffetId = UUID.randomUUID();
        ProductoEntity entity = new ProductoEntity();
        entity.setId(UUID.randomUUID());
        entity.setNombre("Menos vendido");
        entity.setPrecio(new java.math.BigDecimal("100.00"));
        when(springDataRepository.findBuffetTop5LeastSoldProducts(buffetId))
                .thenReturn(List.of(entity));

        List<ProductChangeRecommendation> result =
                productRepository.findBuffetTop5LeastSoldProducts(buffetId);

        assertEquals(1, result.size());
        assertEquals(entity.getId(), result.get(0).getId());
        assertEquals(buffetId, result.get(0).getIdBuffet());
    }

    @Test
    void findBestSoldProductsMapsDomains() {
        ProductoEntity entity = new ProductoEntity();
        entity.setId(UUID.randomUUID());
        entity.setNombre("Mas vendido");
        when(springDataRepository.findBestSoldProducts()).thenReturn(List.of(entity));

        List<Producto> result = productRepository.findBestSoldProducts();

        assertEquals(1, result.size());
        assertEquals("Mas vendido", result.get(0).getNombre());
    }

    @Test
    void updatePriceDelegatesToSpringDataRepository() {
        UUID productId = UUID.randomUUID();
        java.math.BigDecimal newPrice = new java.math.BigDecimal("1200.00");

        productRepository.updatePrice(productId, newPrice);

        verify(springDataRepository).updatePrice(productId, newPrice);
    }

    @Test
    void findBuffetTop3MostSold_ShouldMapToDomain() {
        UUID buffetId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        ProductoEntity entity = new ProductoEntity();
        entity.setId(UUID.randomUUID());
        entity.setNombre("Most Sold");
        when(springDataRepository.findBuffetTop3MostSold(eq(buffetId), eq(categoryId), any()))
                .thenReturn(List.of(entity));

        List<Producto> result = productRepository.findBuffetTop3MostSold(buffetId, categoryId);

        assertEquals(1, result.size());
        assertEquals("Most Sold", result.get(0).getNombre());
    }

    @Test
    void integerValue_ShouldHandleNonNumber() {
        UUID buffetId = UUID.randomUUID();
        ProductoEntity entity = new ProductoEntity();
        entity.setId(UUID.randomUUID());
        ProductStockResult resultObj = stockResult(
                entity,
                null, // "not a number" ya no es posible por tipos, usamos null
                5,
                3,
                EstadoInventario.DISPONIBLE,
                TipoManejoInventario.STOCK_EXACTO,
                20,
                45
        );

        when(springDataRepository.findAllActiveWithStockParams(buffetId))
                .thenReturn(java.util.Collections.singletonList(resultObj));

        List<Producto> result = productRepository.findAllActiveWithStock(buffetId);
        assertNull(result.get(0).getStockActual());
    }

    @Test
    void save_ShouldCreateNewEntity_WhenIdIsNull() {
        Producto producto = new Producto();
        producto.setNombre("New Product");
        producto.setActivo(true);
        CategoriaProducto cat = new CategoriaProducto();
        cat.setId(UUID.randomUUID());
        producto.setCategoria(cat);

        when(springDataRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        productRepository.save(producto);

        verify(springDataRepository).save(argThat(entity -> entity.getId() == null));
    }

    @Test
    void save_ShouldClearCollections_WhenDomainCollectionsAreNull() {
        UUID id = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(id);
        producto.setMedidas(null);
        producto.setTipos(null);
        producto.setClasificacionesSalud(null);
        CategoriaProducto cat = new CategoriaProducto();
        cat.setId(UUID.randomUUID());
        producto.setCategoria(cat);

        ProductoEntity entity = new ProductoEntity();
        entity.setId(id);
        entity.getMedidas().add(new ProductoMedidaEntity());
        entity.getTipos().add(new ClasificacionTipoEntity());
        entity.getClasificacionesSalud().add(new ClasificacionSaludEntity());

        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));
        when(springDataRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        productRepository.save(producto);

        assertTrue(entity.getMedidas().isEmpty());
        assertTrue(entity.getTipos().isEmpty());
        assertTrue(entity.getClasificacionesSalud().isEmpty());
    }

    private ProductStockResult stockResult(
            ProductoEntity entity,
            Number stockActual,
            Number stockReservado,
            Number stockMinimo,
            EstadoInventario estado,
            TipoManejoInventario tipoManejo,
            Number cupoMaximoDiario,
            Number stockDisponible
    ) {
        return new ProductStockResult(
                entity,
                stockActual,
                stockReservado,
                stockMinimo,
                estado,
                tipoManejo,
                cupoMaximoDiario,
                stockDisponible
        );
    }
}
