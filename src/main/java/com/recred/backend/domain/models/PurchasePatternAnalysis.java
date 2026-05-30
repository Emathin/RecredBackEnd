package com.recred.backend.domain.models;

import java.util.UUID;

public record PurchasePatternAnalysis(

    String summary,
    UUID product_id,
    String productName,
    String purchaseDays
){}
