package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.domain.repositories.PromotionRepository;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.PromotionEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PromotionRepositoryImpl implements PromotionRepository {

    private final PromotionSpringDataRepository repository;

    public PromotionRepositoryImpl(PromotionSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Promotion> findAllActive() {
        return repository.findByStatus(PromotionStatus.ACTIVE).stream()
                .map(PromotionEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Promotion> findById(UUID id) {
        return repository.findById(id)
                .map(PromotionEntity::toDomain);
    }

    @Override
    public Promotion save(Promotion promotion) {
        PromotionEntity entity;
        
        if (promotion.getId() != null) {
            entity = repository.findById(promotion.getId()).orElse(new PromotionEntity());
        } else {
            entity = new PromotionEntity();
        }
        
        entity.setName(promotion.getName());
        entity.setDiscountPercentage(promotion.getDiscountPercentage());
        entity.setStartDate(promotion.getStartDate());
        entity.setEndDate(promotion.getEndDate());
        entity.setStatus(promotion.getStatus());
        
        if (promotion.getProducts() != null) {
            entity.setProducts(promotion.getProducts().stream().map(p -> {
                ProductoEntity productoEntity = new ProductoEntity();
                productoEntity.setId(p.getId());
                return productoEntity;
            }).collect(Collectors.toSet()));
        }
        
        return repository.save(entity).toDomain();
    }
}
