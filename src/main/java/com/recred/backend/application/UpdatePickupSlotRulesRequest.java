package com.recred.backend.application;

import java.util.UUID;

public record UpdatePickupSlotRulesRequest(
        UUID slotId,
        Integer cupoMaximo,
        Integer minutosCorte
) {
}
