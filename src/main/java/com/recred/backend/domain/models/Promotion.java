package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Promotion {

    private static final BigDecimal PERCENTAGE_BASE = new BigDecimal("100");

    private UUID id;
    private String name;
    private BigDecimal discountPercentage;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private PromotionStatus status;
    private List<Producto> products;
    private Buffet buffet;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void validateFor(LocalDate pickupDate, UUID buffetId) {
        if (this.status != PromotionStatus.ACTIVE) {
            throw new IllegalArgumentException("Promotion is not active");
        }
        if (this.buffet != null && !this.buffet.getId().equals(buffetId)) {
            throw new IllegalArgumentException("Promotion does not belong to this buffet");
        }
        
        LocalDateTime startOfDay = pickupDate.atStartOfDay();
        LocalDateTime endOfDay = pickupDate.atTime(23, 59, 59);

        if (this.startDate != null && endOfDay.isBefore(this.startDate)) {
            throw new IllegalArgumentException("Promotion has not started yet");
        }
        if (this.endDate != null && startOfDay.isAfter(this.endDate)) {
            throw new IllegalArgumentException("Promotion has ended");
        }
    }

    public BigDecimal discountMultiplier() {
        return BigDecimal.ONE.subtract(
                discountPercentage.divide(
                        PERCENTAGE_BASE,
                        4,
                        RoundingMode.HALF_UP
                )
        );
    }

    public void updateDetails(
            List<Producto> products,
            BigDecimal discountPercentage,
            String name,
            LocalDateTime startDate,
            LocalDateTime endDate,
            LocalDateTime now
    ) {
        updateProductsIfPresent(products);
        updateDiscountIfPresent(discountPercentage);
        updateNameIfPresent(name);
        updateStartDateIfPresent(startDate);
        updateEndDateIfPresent(endDate);
        validateDateRange();
    }

    private void updateProductsIfPresent(List<Producto> products) {
        if (products == null || products.isEmpty()) {
            return;
        }

        if (products.size() < 2) {
            throw new IllegalArgumentException("A promotion requires at least 2 products.");
        }

        this.products = products;
    }

    private void updateDiscountIfPresent(BigDecimal discountPercentage) {
        if (discountPercentage == null) {
            return;
        }

        if (discountPercentage.compareTo(BigDecimal.ZERO) <= 0
                || discountPercentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100.");
        }

        this.discountPercentage = discountPercentage;
    }

    private void updateNameIfPresent(String name) {
        if (name != null) {
            this.name = name;
        }
    }

    private void updateStartDateIfPresent(LocalDateTime startDate) {
        if (startDate != null) {
            this.startDate = startDate;
        }
    }

    private void updateEndDateIfPresent(LocalDateTime endDate) {
        if (endDate != null) {
            this.endDate = endDate;
        }
    }

    private void validateDateRange() {
        if (startDate == null || endDate == null || !startDate.isBefore(endDate)) {
            throw new IllegalArgumentException("Invalid date range.");
        }
    }

    public void toggleStatus(LocalDateTime now) {
        if (this.status == PromotionStatus.INACTIVE) {
            this.status = PromotionStatus.ACTIVE;
        } else {
            this.status = PromotionStatus.INACTIVE;
        }
    }
}
