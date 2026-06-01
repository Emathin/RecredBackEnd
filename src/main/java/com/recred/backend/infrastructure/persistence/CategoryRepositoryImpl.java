package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.repositories.CategoryRepository;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CategoryRepositoryImpl implements CategoryRepository {

    private final SpringDataCategoryRepository springDataRepository;

    public CategoryRepositoryImpl(SpringDataCategoryRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<CategoriaProducto> findById(UUID id) {
        return springDataRepository.findById(id).map(CategoriaProductoEntity::toDomain);
    }

    @Override
    public Optional<CategoriaProducto> findByDescription(String description) {
        return springDataRepository.findByDescripcion(description)
                .map(CategoriaProductoEntity::toDomain);
    }

    @Override
    public CategoriaProducto save(CategoriaProducto category) {
        CategoriaProductoEntity entity = new CategoriaProductoEntity();
        if (category.getId() != null) {
            entity = springDataRepository.findById(category.getId())
                    .orElse(new CategoriaProductoEntity());
        }
        entity.setDescripcion(category.getDescripcion());
        entity.setActivo(category.getActivo());
        return springDataRepository.save(entity).toDomain();
    }

    @Override
    public List<CategoriaProducto> getAllCategories() {
        return springDataRepository.findAll().stream()
                .map(CategoriaProductoEntity::toDomain)
                .collect(Collectors.toList());
    }
}
