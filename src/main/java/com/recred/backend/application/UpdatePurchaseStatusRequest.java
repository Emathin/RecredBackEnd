package com.recred.backend.application;
import com.recred.backend.domain.models.EstadoCompra;
import java.util.UUID;
public record UpdatePurchaseStatusRequest(UUID purchaseId, EstadoCompra requestedStatus) {}
