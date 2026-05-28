package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.EstadoCompra;

public record UpdatePurchaseStatusRequestDTO(EstadoCompra status) {
}
