package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.repositories.InventoryRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InventoryRepositoryImpl implements InventoryRepository {

    private final SpringDataInventoryRepository springDataInventoryRepository;

    public InventoryRepositoryImpl(SpringDataInventoryRepository springDataInventoryRepository) {
        this.springDataInventoryRepository = springDataInventoryRepository;
    }

    @Override
    public boolean checkIfProductIsInStock(UUID productId, UUID buffetId) {
        return springDataInventoryRepository
                .existsByProductIdAndKiosqueroId(productId, buffetId);
    }
}
