package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.*;
import com.recred.backend.infrastructure.entities.*;
import org.junit.jupiter.api.BeforeEach;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.Product;
import com.recred.backend.infrastructure.entities.ProductEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        ProductEntity entity = new ProductEntity();
        entity.setId(id);
        entity.setName("Producto Test");

        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<Product> result = repositoryImpl.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        assertEquals("Producto Test", result.get().getName());
    }

    @Test
    void findById_DeberiaRetornarVacio_CuandoNoExiste() {
        UUID id = UUID.randomUUID();
        when(springDataRepository.findById(id)).thenReturn(Optional.empty());

        Optional<Product> result = productRepository.findById(id);

        assertTrue(result.isEmpty());
    }

    @Test
    void save_DeberiaCrearNuevaEntidad_CuandoProductoNoTieneId() {
        Product product = new Product();
        product.setName("Nuevo");
        ProductCategory cat = new ProductCategory();
        cat.setId(UUID.randomUUID());
        product.setCategory(cat);

        ProductEntity savedEntity = new ProductEntity();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setName("Nuevo");
        when(springDataRepository.save(any(ProductEntity.class))).thenReturn(savedEntity);

        Product result = productRepository.save(product);

        assertNotNull(result.getId());
        verify(springDataRepository).save(any(ProductEntity.class));
    }

    @Test
    void save_DeberiaActualizarEntidadExistente_CuandoProductoTieneId() {
        UUID id = UUID.randomUUID();
        Product product = new Product();
        product.setId(id);
        product.setName("Actualizado");
        ProductCategory cat = new ProductCategory();
        cat.setId(UUID.randomUUID());
        product.setCategory(cat);

        ProductEntity existingEntity = new ProductEntity();
        existingEntity.setId(id);
        existingEntity.setName("Viejo");

        when(springDataRepository.findById(id)).thenReturn(Optional.of(existingEntity));
        when(springDataRepository.save(any(ProductEntity.class))).thenAnswer(i -> i.getArgument(0));

        productRepository.save(product);

        ArgumentCaptor<ProductEntity> captor = ArgumentCaptor.forClass(ProductEntity.class);
        verify(springDataRepository).save(captor.capture());
        assertEquals("Actualizado", captor.getValue().getName());
    }

    @Test
    void save_DeberiaMapearTodo_IncluyendoMedidasTiposYClasificaciones() {
        Product product = new Product();
        product.setName("Completo");

        ProductCategory cat = new ProductCategory();
        cat.setId(UUID.randomUUID());
        product.setCategory(cat);

        ProductMeasure medida = new ProductMeasure();
        medida.setId(UUID.randomUUID());
        product.addMeasure(medida);

        TypeClassification tipo = new TypeClassification();
        tipo.setId(UUID.randomUUID());
        product.setTypes(Set.of(tipo));

        HealthClassification salud = new HealthClassification();
        salud.setId(UUID.randomUUID());
        product.setHealthClassifications(Set.of(salud));

        when(springDataRepository.save(any(ProductEntity.class))).thenAnswer(i -> i.getArgument(0));

        productRepository.save(product);

        ArgumentCaptor<ProductEntity> captor = ArgumentCaptor.forClass(ProductEntity.class);
        verify(springDataRepository).save(captor.capture());
        ProductEntity saved = captor.getValue();

        assertNotNull(saved.getCategory());
        assertFalse(saved.getMeasures().isEmpty());
        assertFalse(saved.getTypes().isEmpty());
        assertFalse(saved.getHealthClassifications().isEmpty());
    }

    @Test
    void save_DeberiaLimpiarColecciones_CuandoVienenVacias() {
        UUID id = UUID.randomUUID();
        Product product = new Product();
        product.setId(id);
        ProductCategory cat = new ProductCategory();
        cat.setId(UUID.randomUUID());
        product.setCategory(cat);

        ProductEntity entity = new ProductEntity();
        entity.setId(id);
        entity.getMeasures().add(new ProductMeasureEntity());
        entity.getTypes().add(new TypeClassificationEntity());
        entity.getHealthClassifications().add(new HealthClassificationEntity());

        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));
        when(springDataRepository.save(any(ProductEntity.class))).thenAnswer(i -> i.getArgument(0));

        productRepository.save(product);

        assertTrue(entity.getMeasures().isEmpty());
        assertTrue(entity.getTypes().isEmpty());
        assertTrue(entity.getHealthClassifications().isEmpty());
    }

    @Test
    void findBuffetLeastSoldProductsReturnsAProduct() {
        UUID buffetId = java.util.UUID.randomUUID();
        ProductEntity entity = new ProductEntity();
        entity.setId(java.util.UUID.randomUUID());
        entity.setName("Menos Vendido");
        entity.setPrice(new java.math.BigDecimal("100.00"));

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
        Product product = new Product();
        product.setId(id);
        ProductCategory cat = new ProductCategory();
        cat.setId(catId);
        product.setCategory(cat);

        ProductEntity entity = new ProductEntity();
        entity.setId(id);
        ProductCategoryEntity catEntity = new ProductCategoryEntity();
        catEntity.setId(catId);
        entity.setCategory(catEntity);

        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));
        when(springDataRepository.save(any(ProductEntity.class))).thenAnswer(i -> i.getArgument(0));

        productRepository.save(product);

        assertSame(catEntity, entity.getCategory());
    }

    @Test
    void save_DeberiaLimpiarColecciones_CuandoVienenNulasEnModelo() {
        UUID id = UUID.randomUUID();
        Product product = new Product();
        product.setId(id);
        product.setMeasures(null);
        product.setTypes(null);
        product.setHealthClassifications(null);
        ProductCategory cat = new ProductCategory();
        cat.setId(UUID.randomUUID());
        product.setCategory(cat);

        ProductEntity entity = new ProductEntity();
        entity.setId(id);

        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));
        when(springDataRepository.save(any(ProductEntity.class))).thenAnswer(i -> i.getArgument(0));

        productRepository.save(product);

        verify(springDataRepository).save(any(ProductEntity.class));
    }

    @Test
    void save_DeberiaLimpiarColecciones_CuandoVienenVaciasEnModelo() {
        UUID id = UUID.randomUUID();
        Product product = new Product();
        product.setId(id);
        product.setMeasures(new java.util.HashSet<>());
        product.setTypes(new java.util.HashSet<>());
        product.setHealthClassifications(new java.util.HashSet<>());
        ProductCategory cat = new ProductCategory();
        cat.setId(UUID.randomUUID());
        product.setCategory(cat);

        ProductEntity entity = new ProductEntity();
        entity.setId(id);

        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));
        when(springDataRepository.save(any(ProductEntity.class))).thenAnswer(i -> i.getArgument(0));

        productRepository.save(product);

        verify(springDataRepository).save(any(ProductEntity.class));
    }

    @Test
    void save_DeberiaCrearNuevaEntidad_CuandoIdNoExisteEnBaseDeDatos() {
        UUID id = UUID.randomUUID();
        Product product = new Product();
        product.setId(id);
        product.setName("Nuevo");
        ProductCategory cat = new ProductCategory();
        cat.setId(UUID.randomUUID());
        product.setCategory(cat);

        when(springDataRepository.findById(id)).thenReturn(Optional.empty());
        when(springDataRepository.save(any(ProductEntity.class))).thenAnswer(i -> i.getArgument(0));

        productRepository.save(product);

        verify(springDataRepository).save(any(ProductEntity.class));
    }

    @Test
    void save_DeberiaCambiarCategoria_CuandoIdEsDiferente() {
        UUID id = UUID.randomUUID();
        UUID catId1 = UUID.randomUUID();
        UUID catId2 = UUID.randomUUID();
        Product product = new Product();
        product.setId(id);
        ProductCategory cat = new ProductCategory();
        cat.setId(catId2);
        product.setCategory(cat);

        ProductEntity entity = new ProductEntity();
        entity.setId(id);
        ProductCategoryEntity catEntity = new ProductCategoryEntity();
        catEntity.setId(catId1);
        entity.setCategory(catEntity);

        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));
        when(springDataRepository.save(any(ProductEntity.class))).thenAnswer(i -> i.getArgument(0));

        productRepository.save(product);

        assertEquals(catId2, entity.getCategory().getId());
    }
}
