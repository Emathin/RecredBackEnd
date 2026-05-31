package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ProductCategory;
import com.recred.backend.domain.repositories.CategoryRepository;
import com.recred.backend.infrastructure.entities.ProductCategoryEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class CategoryRepositoryImpl implements CategoryRepository {

    private final SpringDataCategoryRepository springDataRepository;

    public CategoryRepositoryImpl(SpringDataCategoryRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<ProductCategory> findById(UUID id) {
        return springDataRepository.findById(id).map(ProductCategoryEntity::toDomain);
    }

    @Override
    public Optional<ProductCategory> findByDescription(String description) {
        return springDataRepository.findByDescripcion(description)
                .map(ProductCategoryEntity::toDomain);
    }

    @Override
    public ProductCategory save(ProductCategory category) {
        ProductCategoryEntity entity = new ProductCategoryEntity();
        if (category.getId() != null) {
            entity = springDataRepository.findById(category.getId())
                    .orElse(new ProductCategoryEntity());
        }
        entity.setDescription(category.getDescription());
        entity.setActive(category.getActive());
        return springDataRepository.save(entity).toDomain();
    }
}
