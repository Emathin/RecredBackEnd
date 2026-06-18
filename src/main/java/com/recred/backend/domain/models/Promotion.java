package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
