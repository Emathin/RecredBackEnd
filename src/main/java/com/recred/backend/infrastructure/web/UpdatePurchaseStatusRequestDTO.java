package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.PurchaseState;

public record UpdatePurchaseStatusRequestDTO(PurchaseState status) {
}
