package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductRepositoryImplTest {
    @Mock
    private SpringDataProductRepository springDataRepository;

    @InjectMocks
    private ProductRepositoryImpl repositoryImpl;

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

}
