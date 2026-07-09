package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.domain.repositories.PromotionRepository;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.PromotionEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PromotionRepositoryImpl implements PromotionRepository {

    private final PromotionSpringDataRepository repository;
    private final SpringDataBuffetRepository buffetRepository;

    public PromotionRepositoryImpl(PromotionSpringDataRepository repository,
                                   SpringDataBuffetRepository buffetRepository) {
        this.repository = repository;
        this.buffetRepository = buffetRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Promotion> findAllActive() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return repository.findByStatus(PromotionStatus.ACTIVE).stream()
                .filter(entity -> entity.getEndDate() == null || !now.isAfter(entity.getEndDate()))
                .peek(entity -> {
                    if (entity.getProducts() != null) {
                        entity.getProducts().size();
                    }
                })
                .map(PromotionEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Promotion> findAllActiveByBuffet(UUID buffetId) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return repository.findByStatusAndBuffetEntityId(PromotionStatus.ACTIVE, buffetId).stream()
                .filter(entity -> entity.getEndDate() == null || !now.isAfter(entity.getEndDate()))
                .peek(entity -> {
                    if (entity.getProducts() != null) {
                        entity.getProducts().size();
                    }
                })
                .map(PromotionEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Promotion> findAllByBuffet(UUID buffetId) {
        return repository.findByBuffetEntityId(buffetId).stream()
                .peek(entity -> {
                    if (entity.getProducts() != null) {
                        entity.getProducts().size();
                    }
                })
                .map(PromotionEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Promotion> findInactivePromotionsByStartDateBefore(java.time.LocalDateTime date) {
        return repository.findByStatusAndStartDateBefore(PromotionStatus.INACTIVE, date).stream()
                .peek(entity -> {
                    if (entity.getProducts() != null) {
                        entity.getProducts().size();
                    }
                })
                .map(PromotionEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Promotion> findById(UUID id) {
        return repository.findById(id)
                .map(entity -> {
                    if (entity.getProducts() != null) {
                        entity.getProducts().size();
                    }
                    return entity.toDomain();
                });
    }

    @Override
    @Transactional
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
        entity.setImageUrl(promotion.getImageUrl());
        
        if (promotion.getProducts() != null) {
            Set<ProductoEntity> newProducts = promotion.getProducts().stream().map(p -> {
                ProductoEntity productoEntity = new ProductoEntity();
                productoEntity.setId(p.getId());
                return productoEntity;
            }).collect(Collectors.toSet());
            
            if (entity.getProducts() == null) {
                entity.setProducts(newProducts);
            } else {
                entity.getProducts().clear();
                entity.getProducts().addAll(newProducts);
            }
        }
        
        if (promotion.getBuffet() != null) {
            buffetRepository.findById(promotion.getBuffet().getId())
                    .ifPresent(entity::setBuffetEntity);
        }
        
        return repository.save(entity).toDomain();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
