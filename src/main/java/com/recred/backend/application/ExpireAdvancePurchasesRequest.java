package com.recred.backend.application;

import java.time.LocalDateTime;
import java.util.UUID;

public record ExpireAdvancePurchasesRequest(
        UUID buffetId,
        LocalDateTime targetTime
) {
}
