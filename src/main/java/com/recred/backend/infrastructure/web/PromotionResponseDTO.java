package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Promotion;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
public class PromotionResponseDTO {
    private UUID id;
    private String name;
    private BigDecimal discountPercentage;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private List<UUID> productIds;
    private UUID buffetId;
    private String imageUrl;

    public static PromotionResponseDTO fromAppResponse(
            com.recred.backend.application.PromotionResponse res) {
        PromotionResponseDTO dto = new PromotionResponseDTO();
        dto.setId(res.id());
        dto.setName(res.name());
        dto.setDiscountPercentage(res.discountPercentage());
        dto.setStartDate(res.startDate());
        dto.setEndDate(res.endDate());
        dto.setStatus(res.status());
        dto.setImageUrl(res.imageUrl());
        dto.setProductIds(res.productIds());
        dto.setBuffetId(res.buffetId());
        return dto;
    }

    public static PromotionResponseDTO fromDomain(Promotion promotion) {
        PromotionResponseDTO dto = new PromotionResponseDTO();
        dto.setId(promotion.getId());
        dto.setName(promotion.getName());
        dto.setDiscountPercentage(promotion.getDiscountPercentage());
        dto.setStartDate(promotion.getStartDate());
        dto.setEndDate(promotion.getEndDate());
        dto.setStatus(promotion.getStatus().name());
        dto.setImageUrl(promotion.getImageUrl());
        
        if (promotion.getProducts() != null) {
            dto.setProductIds(promotion.getProducts().stream()
                    .map(p -> p.getId())
                    .collect(Collectors.toList()));
        }
        
        if (promotion.getBuffet() != null) {
            dto.setBuffetId(promotion.getBuffet().getId());
        }
        
        return dto;
    }
}
