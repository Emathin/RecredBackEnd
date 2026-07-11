package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.persistence.CategoryRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataCategoryRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class CategoryRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private SpringDataCategoryRepository springDataRepository;

    @Autowired
    private CategoryRepositoryImpl categoryRepository;

    private CategoriaProductoEntity cachedCategory;

    @Test
    void findById_ShouldReturnCategory_WhenExists() {
        CategoriaProductoEntity entity = givenCategoryEntity();

        Optional<CategoriaProducto> result = whenFindingCategoryById(entity.getId());

        thenCategoryIsReturned(result, entity);
    }

    @Test
    void findByDescription_ShouldReturnCategory_WhenExists() {
        CategoriaProductoEntity entity = givenCategoryEntity();

        Optional<CategoriaProducto> result = whenFindingCategoryByDescription(entity.getDescripcion());

        thenCategoryIsReturned(result, entity);
    }

    @Test
    void save_ShouldCreateNewEntity_WhenIdIsNull() {
        CategoriaProducto category = givenCategoryDomain("New_Category");

        CategoriaProducto savedCategory = whenSavingCategory(category);

        thenCategoryIsPersisted(savedCategory.getId(), "New_Category");
    }

    @Test
    void save_ShouldUpdateEntity_WhenIdExists() {
        CategoriaProductoEntity existingEntity = givenCategoryEntity();
        CategoriaProducto category = givenCategoryDomain("Updated_Category");
        category.setId(existingEntity.getId());

        whenSavingCategory(category);

        thenCategoryIsPersisted(existingEntity.getId(), "Updated_Category");
    }

    @Test
    void getAllCategories_ShouldMapAllEntities() {
        givenCategoryEntity();

        List<CategoriaProducto> result = whenGettingAllCategories();

        thenCategoryIsPresentInList(result);
    }

    private CategoriaProductoEntity givenCategoryEntity() {
        return generateCategoryEntity();
    }

    private CategoriaProducto givenCategoryDomain(String description) {
        return createCategoryDomain(description);
    }

    private Optional<CategoriaProducto> whenFindingCategoryById(UUID id) {
        return categoryRepository.findById(id);
    }

    private Optional<CategoriaProducto> whenFindingCategoryByDescription(String description) {
        return categoryRepository.findByDescription(description);
    }

    private CategoriaProducto whenSavingCategory(CategoriaProducto category) {
        return categoryRepository.save(category);
    }

    private List<CategoriaProducto> whenGettingAllCategories() {
        return categoryRepository.getAllCategories();
    }

    private void thenCategoryIsReturned(Optional<CategoriaProducto> result, CategoriaProductoEntity entity) {
        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
        assertEquals(entity.getDescripcion(), result.get().getDescripcion());
    }

    private void thenCategoryIsPersisted(UUID id, String expectedDescription) {
        Optional<CategoriaProductoEntity> entityOpt = springDataRepository.findById(id);
        assertTrue(entityOpt.isPresent());
        assertEquals(expectedDescription, entityOpt.get().getDescripcion());
    }

    private void thenCategoryIsPresentInList(List<CategoriaProducto> result) {
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(c -> c.getDescripcion().equals(cachedCategory.getDescripcion())));
    }

    private CategoriaProductoEntity generateCategoryEntity() {
        if (this.cachedCategory == null) {
            CategoriaProductoEntity entity = new CategoriaProductoEntity();
            entity.setDescripcion("Beverages");
            entity.setActivo(true);
            this.cachedCategory = springDataRepository.saveAndFlush(entity);
        }
        return this.cachedCategory;
    }

    private CategoriaProducto createCategoryDomain(String description) {
        CategoriaProducto category = new CategoriaProducto();
        category.setDescripcion(description);
        category.setActivo(true);
        return category;
    }
}
