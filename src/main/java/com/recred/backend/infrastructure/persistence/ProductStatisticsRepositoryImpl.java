package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.repositories.ProductStatisticsRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class ProductStatisticsRepositoryImpl implements ProductStatisticsRepository {

    private final ProductStatisticsJpaRepository jpaRepository;

    public ProductStatisticsRepositoryImpl(ProductStatisticsJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Long countTotalSalesByProduct(UUID productoId, LocalDateTime since) {
        return jpaRepository.countTotalSalesByProduct(productoId, since);
    }

    @Override
    public Long countDistinctCustomersByProduct(UUID productoId, LocalDateTime since) {
        return jpaRepository.countDistinctCustomersByProduct(productoId, since);
    }

    @Override
    public BigDecimal calculateTotalRevenueByProduct(UUID productoId, LocalDateTime since) {
        return jpaRepository.calculateTotalRevenueByProduct(productoId, since);
    }
}
