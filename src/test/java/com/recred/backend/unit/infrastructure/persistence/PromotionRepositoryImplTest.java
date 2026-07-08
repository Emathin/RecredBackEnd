package com.recred.backend.unit.infrastructure.persistence;

import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.PromotionEntity;
import com.recred.backend.infrastructure.persistence.PromotionRepositoryImpl;
import com.recred.backend.infrastructure.persistence.PromotionSpringDataRepository;
import com.recred.backend.infrastructure.persistence.SpringDataBuffetRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionRepositoryImplTest {

    @Mock
    private PromotionSpringDataRepository repository;
    @Mock
    private SpringDataBuffetRepository buffetRepository;

    private PromotionRepositoryImpl adapter;

    @BeforeEach
    void setUp() {
        adapter = new PromotionRepositoryImpl(repository, buffetRepository);
    }

    @Test
    void findMethodsShouldMapRepositoryEntities() {
        UUID buffetId = UUID.randomUUID();
        UUID promotionId = UUID.randomUUID();
        LocalDateTime startDate = LocalDateTime.of(2026, 7, 4, 12, 0);
        PromotionEntity entity = entity(promotionId);
        when(repository.findByStatusAndBuffetEntityId(PromotionStatus.ACTIVE, buffetId))
                .thenReturn(List.of(entity));
        when(repository.findByStatusAndStartDateBefore(PromotionStatus.DRAFT, startDate))
                .thenReturn(List.of(entity));

        List<Promotion> activeByBuffet = adapter.findAllActiveByBuffet(buffetId);
        List<Promotion> drafts = adapter.findDraftPromotionsByStartDateBefore(startDate);

        assertEquals(promotionId, activeByBuffet.get(0).getId());
        assertEquals(promotionId, drafts.get(0).getId());
    }

    @Test
    void saveShouldUpdateExistingEntityAndAttachBuffet() {
        UUID promotionId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        PromotionEntity existing = new PromotionEntity();
        BuffetEntity buffetEntity = new BuffetEntity();
        buffetEntity.setId(buffetId);
        when(repository.findById(promotionId)).thenReturn(Optional.of(existing));
        when(buffetRepository.findById(buffetId)).thenReturn(Optional.of(buffetEntity));
        when(repository.save(any(PromotionEntity.class))).thenAnswer(invocation -> {
            PromotionEntity saved = invocation.getArgument(0);
            saved.setId(promotionId);
            return saved;
        });

        Promotion result = adapter.save(domain(promotionId, productId, buffetId));

        ArgumentCaptor<PromotionEntity> captor = ArgumentCaptor.forClass(PromotionEntity.class);
        verify(repository).save(captor.capture());
        assertEquals("Promo", captor.getValue().getName());
        assertEquals(buffetEntity, captor.getValue().getBuffetEntity());
        assertEquals(1, captor.getValue().getProducts().size());
        assertEquals(promotionId, result.getId());
        assertTrue(result.getProducts().stream()
                .anyMatch(product -> productId.equals(product.getId())));
    }

    private Promotion domain(UUID promotionId, UUID productId, UUID buffetId) {
        Producto product = new Producto();
        product.setId(productId);
        Buffet buffet = new Buffet();
        buffet.setId(buffetId);
        Promotion promotion = new Promotion();
        promotion.setId(promotionId);
        promotion.setName("Promo");
        promotion.setDiscountPercentage(new BigDecimal("10.00"));
        promotion.setStartDate(LocalDateTime.of(2026, 7, 4, 8, 0));
        promotion.setEndDate(LocalDateTime.of(2099, 7, 5, 20, 0));
        promotion.setStatus(PromotionStatus.ACTIVE);
        promotion.setImageUrl("promo.png");
        promotion.setProducts(List.of(product));
        promotion.setBuffet(buffet);
        return promotion;
    }

    private PromotionEntity entity(UUID promotionId) {
        PromotionEntity entity = new PromotionEntity();
        entity.setId(promotionId);
        entity.setName("Promo");
        entity.setDiscountPercentage(new BigDecimal("10.00"));
        entity.setStartDate(LocalDateTime.of(2026, 7, 4, 8, 0));
        entity.setEndDate(LocalDateTime.of(2099, 7, 5, 20, 0));
        entity.setStatus(PromotionStatus.ACTIVE);
        return entity;
    }
}
