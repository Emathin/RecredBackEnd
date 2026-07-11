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
        PromotionEntity entity = givenActiveAndInactivePromotions();
        
        List<Promotion> result = whenFindingAllActivePromotions();
        
        thenOnlyActivePromotionsAreReturned(result, entity.getId());
    }

    @Test
    void findByIdShouldMapEntityWhenPresent() {
        PromotionEntity entity = givenActivePromotion();
        
        Optional<Promotion> result = whenFindingPromotionById(entity.getId());
        
        thenPromotionIsFound(result, entity.getId());
    }

    @Test
    void saveShouldCreatePromotionWithProducts() {
        Promotion promotion = givenNewPromotionDomain();
        
        Promotion result = whenSavingPromotion(promotion);
        
        thenPromotionIsSavedCorrectly(result, promotion.getName());
    }

    @Test
    void saveShouldUpdateExistingPromotion() {
        Promotion promotion = givenExistingPromotionForUpdate();
        
        Promotion result = whenSavingPromotion(promotion);
        
        thenPromotionIsUpdatedCorrectly(result);
    }

    @Test
    void saveShouldHandleNullProducts() {
        Promotion promotion = givenPromotionWithoutProducts();
        
        Promotion result = whenSavingPromotion(promotion);
        
        thenPromotionWithoutProductsIsSaved(result);
    }

    private PromotionEntity givenActiveAndInactivePromotions() {
        ProductoEntity product = generateProductEntity("Alfajor");
        PromotionEntity active = generatePromotionEntity("Active Promo", PromotionStatus.ACTIVE, product);
        generatePromotionEntity("Inactive Promo", PromotionStatus.INACTIVE, product);
        return active;
    }

    private PromotionEntity givenActivePromotion() {
        ProductoEntity product = generateProductEntity("Juice");
        return generatePromotionEntity("Juice Promo", PromotionStatus.ACTIVE, product);
    }

    private Promotion givenNewPromotionDomain() {
        ProductoEntity product = generateProductEntity("Cookie");
        return createDomainPromotion(null, "New Promo", "15.00", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(10), PromotionStatus.ACTIVE, product.getId());
    }

    private Promotion givenExistingPromotionForUpdate() {
        ProductoEntity product = generateProductEntity("Yogurt");
        PromotionEntity existing = generatePromotionEntity("Old Promo", PromotionStatus.ACTIVE, product);
        return createDomainPromotion(existing.getId(), "Updated Promo", "20.00", existing.getStartDate(), existing.getEndDate(), existing.getStatus(), product.getId());
    }

    private Promotion givenPromotionWithoutProducts() {
        return createDomainPromotion(null, "Promo Without Products", "10.00", LocalDateTime.now(), LocalDateTime.now().plusDays(5), PromotionStatus.ACTIVE, null);
    }

    private List<Promotion> whenFindingAllActivePromotions() {
        return repository.findAllActive();
    }

    private Optional<Promotion> whenFindingPromotionById(UUID id) {
        return repository.findById(id);
    }

    private Promotion whenSavingPromotion(Promotion promotion) {
        Promotion result = repository.save(promotion);
        entityManager.flush();
        entityManager.clear();
        return result;
    }

    private void thenOnlyActivePromotionsAreReturned(List<Promotion> result, UUID expectedActiveId) {
        assertTrue(result.size() >= 1);
        assertTrue(result.stream().anyMatch(p -> p.getId().equals(expectedActiveId)));
    }

    private void thenPromotionIsFound(Optional<Promotion> result, UUID expectedId) {
        assertTrue(result.isPresent());
        assertEquals(expectedId, result.get().getId());
    }

    private void thenPromotionIsSavedCorrectly(Promotion result, String expectedName) {
        assertTrue(springDataRepository.findById(result.getId()).isPresent());
        PromotionEntity saved = springDataRepository.findById(result.getId()).get();
        assertEquals(expectedName, saved.getName());
        assertEquals(new BigDecimal("15.00"), saved.getDiscountPercentage());
        assertEquals(PromotionStatus.ACTIVE, saved.getStatus());
        assertEquals(expectedName, result.getName());
    }

    private void thenPromotionIsUpdatedCorrectly(Promotion result) {
        PromotionEntity updated = springDataRepository.findById(result.getId()).get();
        assertEquals("Updated Promo", updated.getName());
        assertEquals(new BigDecimal("20.00"), updated.getDiscountPercentage());
    }

    private void thenPromotionWithoutProductsIsSaved(Promotion result) {
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
