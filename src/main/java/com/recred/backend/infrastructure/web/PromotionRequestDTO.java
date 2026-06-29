package com.recred.backend.infrastructure.web;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PromotionRequestDTO {
    private String name;
    private BigDecimal discountPercentage;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<UUID> productIds;
    private UUID buffetId;
    private String imageUrl;
}
