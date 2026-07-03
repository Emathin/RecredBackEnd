package com.recred.backend.application;

import java.util.UUID;

public record GetProductStatisticsRequest(UUID productId, UUID kiosqueroId) {
}
