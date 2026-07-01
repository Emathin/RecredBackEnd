package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Promotion {

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

}
