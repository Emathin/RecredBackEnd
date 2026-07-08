package com.recred.backend.application;

public record ExpireAdvancePurchasesResponse(
        int expiredPurchases,
        int releasedReservations
) {
}
