package com.recred.backend.application;

import java.util.Set;
import java.util.UUID;

public record GetCartSuggestionsRequest(
        UUID studentId,
        UUID buffetId,
        Set<UUID> cartProductIds,
        Integer limit
) {
}
