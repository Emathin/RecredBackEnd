package com.recred.backend.domain.repositories;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface ProductStatisticsRepository {
    Long countTotalSalesByProduct(UUID productoId, LocalDateTime since);
    Long countDistinctCustomersByProduct(UUID productoId, LocalDateTime since);
    BigDecimal calculateTotalRevenueByProduct(UUID productoId, LocalDateTime since);
}
