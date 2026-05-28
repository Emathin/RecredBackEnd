package com.recred.backend.domain.repositories;

import java.util.UUID;

public interface InventoryRepository {
    boolean checkIfProductIsInStock(UUID productId, UUID buffetId);
}
