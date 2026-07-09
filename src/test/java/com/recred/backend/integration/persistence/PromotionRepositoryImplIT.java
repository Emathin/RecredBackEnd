package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.PromotionEntity;
import com.recred.backend.infrastructure.persistence.PromotionRepositoryImpl;
import com.recred.backend.infrastructure.persistence.PromotionSpringDataRepository;
import com.recred.backend.infrastructure.persistence.SpringDataProductRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class PromotionRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private PromotionRepositoryImpl repository;

    @Autowired
    private PromotionSpringDataRepository springDataRepository;

    @Autowired
    private SpringDataProductRepository productRepository;
    
    @Autowired
    private EntityManager entityManager;

    @Test
    void findAllActiveShouldMapEntities() {
        ProductoEntity product = generateProductEntity("Alfajor");
        PromotionEntity entity = generatePromotionEntity("Active Promo", PromotionStatus.ACTIVE, product);
        generatePromotionEntity("Inactive Promo", PromotionStatus.INACTIVE, product);

        List<Promotion> result = repository.findAllActive();

        assertTrue(result.size() >= 1);
        assertTrue(result.stream().anyMatch(p -> p.getId().equals(entity.getId())));
    }

    @Test
    void findByIdShouldMapEntityWhenPresent() {
        ProductoEntity product = generateProductEntity("Juice");
        PromotionEntity entity = generatePromotionEntity("Juice Promo", PromotionStatus.ACTIVE, product);

        Optional<Promotion> result = repository.findById(entity.getId());

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
        assertEquals("Juice Promo", result.get().getName());
    }

    @Test
    void saveShouldCreatePromotionWithProducts() {
        ProductoEntity product = generateProductEntity("Cookie");
        
        Promotion promotion = createDomainPromotion(null, "New Promo", "15.00", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(10), PromotionStatus.ACTIVE, product.getId());

        Promotion result = repository.save(promotion);
        entityManager.flush();
        entityManager.clear();

        assertTrue(springDataRepository.findById(result.getId()).isPresent());
        PromotionEntity saved = springDataRepository.findById(result.getId()).get();
        assertEquals("New Promo", saved.getName());
        assertEquals(new BigDecimal("15.00"), saved.getDiscountPercentage());
        assertEquals(PromotionStatus.ACTIVE, saved.getStatus());
        assertEquals(product.getId(), saved.getProducts().iterator().next().getId());
        assertEquals("New Promo", result.getName());
    }

    @Test
    void saveShouldUpdateExistingPromotion() {
        ProductoEntity product = generateProductEntity("Yogurt");
        PromotionEntity existing = generatePromotionEntity("Old Promo", PromotionStatus.ACTIVE, product);
        
        Promotion promotion = createDomainPromotion(existing.getId(), "Updated Promo", "20.00", existing.getStartDate(), existing.getEndDate(), existing.getStatus(), product.getId());

        Promotion result = repository.save(promotion);
        entityManager.flush();
        entityManager.clear();

        PromotionEntity updated = springDataRepository.findById(existing.getId()).get();
        assertEquals("Updated Promo", updated.getName());
        assertEquals(new BigDecimal("20.00"), updated.getDiscountPercentage());
        assertEquals(product.getId(), updated.getProducts().iterator().next().getId());
        assertEquals(existing.getId(), result.getId());
    }

    @Test
    void saveShouldHandleNullProducts() {
        Promotion promotion = createDomainPromotion(null, "Promo Without Products", "10.00", LocalDateTime.now(), LocalDateTime.now().plusDays(5), PromotionStatus.ACTIVE, null);

        Promotion result = repository.save(promotion);
        entityManager.flush();
        entityManager.clear();

        assertEquals("Promo Without Products", result.getName());
        PromotionEntity saved = springDataRepository.findById(result.getId()).get();
        assertTrue(saved.getProducts().isEmpty());
    }

    private ProductoEntity generateProductEntity(String name) {
        CategoriaProductoEntity category = new CategoriaProductoEntity();
        category.setDescripcion("Cat 1");
        category.setActivo(true);
        entityManager.persist(category);

        ProductoEntity productEntity = new ProductoEntity();
        productEntity.setCategoria(category);
        productEntity.setNombre(name);
        productEntity.setDescripcion("Desc");
        productEntity.setPrecio(new BigDecimal("40.00"));
        productEntity.setActivo(true);
        productEntity.setRequierePreparacion(false);
        return productRepository.save(productEntity);
    }

    private PromotionEntity generatePromotionEntity(String name, PromotionStatus status, ProductoEntity product) {
        PromotionEntity entity = new PromotionEntity();
        entity.setName(name);
        entity.setDiscountPercentage(new BigDecimal("10.00"));
        entity.setStartDate(LocalDateTime.now().minusDays(5));
        entity.setEndDate(LocalDateTime.now().plusDays(15));
        entity.setStatus(status);
        entity.setProducts(new java.util.HashSet<>(List.of(product)));
        return springDataRepository.save(entity);
    }

    private Promotion createDomainPromotion(UUID id, String name, String discount, LocalDateTime start, LocalDateTime end, PromotionStatus status, UUID productId) {
        Promotion promotion = new Promotion();
        promotion.setId(id);
        promotion.setName(name);
        promotion.setDiscountPercentage(new BigDecimal(discount));
        promotion.setStartDate(start);
        promotion.setEndDate(end);
        promotion.setStatus(status);
        if (productId != null) {
            Producto domainProduct = new Producto();
            domainProduct.setId(productId);
            promotion.setProducts(List.of(domainProduct));
        }
        return promotion;
    }
}
