package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.*;
import com.recred.backend.infrastructure.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductRepositoryImplTest {

    @Mock
    private SpringDataProductRepository springDataRepository;

    private ProductRepositoryImpl productRepository;

    @BeforeEach
    void setUp() {
        productRepository = new ProductRepositoryImpl(springDataRepository);
    }

    @Test
    void findById_DeberiaRetornarProductoMapeado_CuandoExiste() {
        UUID id = UUID.randomUUID();
        ProductoEntity entity = new ProductoEntity();
        entity.setId(id);
        entity.setNombre("Test");
        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<Producto> result = productRepository.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        assertEquals("Test", result.get().getNombre());
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
}
