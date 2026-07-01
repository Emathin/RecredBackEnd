package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.RecessTime;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record CreateAssistantAdvancePurchaseRequest(
        UUID studentId,
        UUID buffetId,
        Map<Producto, Integer> cart,
        RecessTime recessTime,
        LocalDate pickupDate
) {
    public void validate() {
        if (studentId == null) {
            throw new IllegalArgumentException("studentId is required");
        }

        if (buffetId == null) {
            throw new IllegalArgumentException("buffetId is required");
        }

        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("cart is required");
        }

        if (recessTime == null) {
            throw new IllegalArgumentException("recessTime is required");
        }

        if (pickupDate == null) {
            throw new IllegalArgumentException("pickupDate is required");
        }
    }
}
