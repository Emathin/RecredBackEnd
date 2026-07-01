package com.recred.backend.application;

import java.util.UUID;

public record GetStockMovementsRequest(UUID buffetId, UUID productId) {
}
