package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.PromotionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionRepositoryImplTest {

    @Mock
    private PromotionSpringDataRepository springDataRepository;

    @Mock
    private SpringDataBuffetRepository buffetRepository;

    private PromotionRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new PromotionRepositoryImpl(springDataRepository, buffetRepository);
    }

    @Test
    void findAllActiveShouldMapEntities() {
        PromotionEntity entity = entity(UUID.randomUUID());
        when(springDataRepository.findByStatus(PromotionStatus.ACTIVE))
                .thenReturn(List.of(entity));

        List<Promotion> result = repository.findAllActive();

        assertEquals(1, result.size());
        assertEquals(entity.getId(), result.get(0).getId());
    }

    @Test
    void findByIdShouldMapEntityWhenPresent() {
        UUID id = UUID.randomUUID();
        PromotionEntity entity = entity(id);
        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<Promotion> result = repository.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.orElseThrow().getId());
    }

    @Test
    void saveShouldCreatePromotionWithProducts() {
        UUID productId = UUID.randomUUID();
        Promotion promotion = promotion(null, productId);
        when(springDataRepository.save(any(PromotionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Promotion result = repository.save(promotion);

        ArgumentCaptor<PromotionEntity> captor = ArgumentCaptor.forClass(PromotionEntity.class);
        verify(springDataRepository).save(captor.capture());
        PromotionEntity saved = captor.getValue();
        assertEquals("Promo", saved.getName());
        assertEquals(new BigDecimal("15.00"), saved.getDiscountPercentage());
        assertEquals(PromotionStatus.ACTIVE, saved.getStatus());
        assertEquals(productId, saved.getProducts().iterator().next().getId());
        assertEquals("Promo", result.getName());
    }

    @Test
    void saveShouldUpdateExistingPromotion() {
        UUID promotionId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Promotion promotion = promotion(promotionId, productId);
        PromotionEntity existing = entity(promotionId);
        when(springDataRepository.findById(promotionId)).thenReturn(Optional.of(existing));
        when(springDataRepository.save(existing)).thenReturn(existing);

        Promotion result = repository.save(promotion);

        assertEquals("Promo", existing.getName());
        assertEquals(productId, existing.getProducts().iterator().next().getId());
        assertEquals(promotionId, result.getId());
        verify(springDataRepository).save(existing);
    }

    @Test
    void saveShouldHandleNullProducts() {
        Promotion promotion = promotion(null, UUID.randomUUID());
        promotion.setProducts(null);
        when(springDataRepository.save(any(PromotionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Promotion result = repository.save(promotion);

        assertEquals("Promo", result.getName());
    }

    private Promotion promotion(UUID id, UUID productId) {
        Producto product = new Producto();
        product.setId(productId);

        Promotion promotion = new Promotion();
        promotion.setId(id);
        promotion.setName("Promo");
        promotion.setDiscountPercentage(new BigDecimal("15.00"));
        promotion.setStartDate(LocalDateTime.of(2026, 6, 1, 0, 0));
        promotion.setEndDate(LocalDateTime.of(2026, 6, 15, 23, 59));
        promotion.setStatus(PromotionStatus.ACTIVE);
        promotion.setProducts(List.of(product));
        return promotion;
    }

    private PromotionEntity entity(UUID id) {
        ProductoEntity product = new ProductoEntity();
        product.setId(UUID.randomUUID());
        product.setNombre("Alfajor");
        product.setDescripcion("Alfajor simple");
        product.setPrecio(new BigDecimal("40.00"));
        product.setActivo(true);
        product.setRequierePreparacion(false);

        PromotionEntity entity = new PromotionEntity();
        entity.setId(id);
        entity.setName("Promo vieja");
        entity.setDiscountPercentage(new BigDecimal("10.00"));
        entity.setStartDate(LocalDateTime.of(2026, 6, 1, 0, 0));
        entity.setEndDate(LocalDateTime.of(2026, 6, 15, 23, 59));
        entity.setStatus(PromotionStatus.ACTIVE);
        entity.setProducts(new java.util.HashSet<>(List.of(product)));
        return entity;
    }
}
