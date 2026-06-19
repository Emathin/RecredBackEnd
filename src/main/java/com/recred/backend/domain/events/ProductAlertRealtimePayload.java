package com.recred.backend.domain.events;

import java.util.UUID;

public record ProductAlertRealtimePayload(UUID productId)
        implements KiosqueroRealtimePayload {
}
