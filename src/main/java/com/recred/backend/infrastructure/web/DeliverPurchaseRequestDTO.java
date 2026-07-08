package com.recred.backend.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

public record DeliverPurchaseRequestDTO(
        @NotBlank String withdrawalCode
) {
}
