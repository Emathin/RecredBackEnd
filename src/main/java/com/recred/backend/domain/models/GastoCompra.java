package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GastoCompra(LocalDateTime fechaHora, BigDecimal monto) {
}
