package com.recred.backend.application;

import java.util.UUID;

public record PrizeHealthyPurchaseCommand(
        UUID alumnoId,
        UUID compraId
) {}
