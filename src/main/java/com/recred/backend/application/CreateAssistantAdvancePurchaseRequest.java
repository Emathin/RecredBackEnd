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
) {}
