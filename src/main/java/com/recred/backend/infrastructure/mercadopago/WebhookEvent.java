package com.recred.backend.infrastructure.mercadopago;

public record WebhookEvent(String paymentId, boolean isPaymentEvent, String notificationId) {
}
