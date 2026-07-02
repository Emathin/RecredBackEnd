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
        CategoriaProductoEntity entity = generateCategoryEntity();

        Optional<CategoriaProducto> result = categoryRepository.findById(entity.getId());

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
        assertEquals(entity.getDescripcion(), result.get().getDescripcion());
    }

    @Test
    void findByDescription_ShouldReturnCategory_WhenExists() {
        CategoriaProductoEntity entity = generateCategoryEntity();

        Optional<CategoriaProducto> result = categoryRepository.findByDescription(entity.getDescripcion());

        assertTrue(result.isPresent());
        assertEquals(entity.getDescripcion(), result.get().getDescripcion());
    }

    @Test
    void save_ShouldCreateNewEntity_WhenIdIsNull() {
        CategoriaProducto category = createCategoryDomain("New_Category");

        CategoriaProducto savedCategory = categoryRepository.save(category);

        Optional<CategoriaProductoEntity> entityOpt = springDataRepository.findById(savedCategory.getId());
        assertTrue(entityOpt.isPresent());
        assertEquals("New_Category", entityOpt.get().getDescripcion());
    }

    @Test
    void save_ShouldUpdateEntity_WhenIdExists() {
        CategoriaProductoEntity existingEntity = generateCategoryEntity();
        CategoriaProducto category = createCategoryDomain("Updated_Category");
        category.setId(existingEntity.getId());

        categoryRepository.save(category);

        Optional<CategoriaProductoEntity> entityOpt = springDataRepository.findById(existingEntity.getId());
        assertTrue(entityOpt.isPresent());
        assertEquals("Updated_Category", entityOpt.get().getDescripcion());
    }

    @Test
    void getAllCategories_ShouldMapAllEntities() {
        generateCategoryEntity();

        List<CategoriaProducto> result = categoryRepository.getAllCategories();

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
