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
        return repository.findByStatus(PromotionStatus.ACTIVE).stream()
                .map(PromotionEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Promotion> findAllActiveByBuffet(UUID buffetId) {
        return repository.findByStatusAndBuffetEntityId(PromotionStatus.ACTIVE, buffetId).stream()
                .map(PromotionEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Promotion> findDraftPromotionsByStartDateBefore(java.time.LocalDateTime date) {
        return repository.findByStatusAndStartDateBefore(PromotionStatus.DRAFT, date).stream()
                .map(PromotionEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Promotion> findById(UUID id) {
        return repository.findById(id)
                .map(PromotionEntity::toDomain);
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
            entity.setProducts(promotion.getProducts().stream().map(p -> {
                ProductoEntity productoEntity = new ProductoEntity();
                productoEntity.setId(p.getId());
                return productoEntity;
            }).collect(Collectors.toSet()));
        }
        
        if (promotion.getBuffet() != null) {
            buffetRepository.findById(promotion.getBuffet().getId())
                    .ifPresent(entity::setBuffetEntity);
        }
        
        return repository.save(entity).toDomain();
    }
}
