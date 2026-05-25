package com.recred.backend.domain.models;

import java.math.BigDecimal;

public record ConsumptionCategory(String description, BigDecimal totalAmount) {
}
