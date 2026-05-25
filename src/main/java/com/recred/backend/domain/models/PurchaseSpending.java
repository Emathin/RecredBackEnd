package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PurchaseSpending(LocalDateTime dateTime, BigDecimal amount) {
}
