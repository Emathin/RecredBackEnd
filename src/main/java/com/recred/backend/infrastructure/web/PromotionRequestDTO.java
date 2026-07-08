package com.recred.backend.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PromotionRequestDTO {
    @NotBlank(message = "Promotion name is required")
    private String name;
    @NotNull(message = "Discount percentage is required")
    private BigDecimal discountPercentage;
    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;
    @NotNull(message = "End date is required")
    private LocalDateTime endDate;
    @NotNull(message = "Products are required")
    private List<UUID> productIds;
    private UUID buffetId;
    private String imageUrl;
}
