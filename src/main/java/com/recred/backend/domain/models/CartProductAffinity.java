package com.recred.backend.domain.models;

import java.util.UUID;

public record CartProductAffinity(
        UUID productId,
        long togetherCount
) {
}
