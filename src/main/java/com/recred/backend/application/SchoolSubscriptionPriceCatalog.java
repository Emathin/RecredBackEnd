package com.recred.backend.application;

import java.math.BigDecimal;

public class SchoolSubscriptionPriceCatalog {

    private static final BigDecimal MONTHLY_PRICE = new BigDecimal("20.00");
    private static final String CURRENCY = "USD";

    public BigDecimal monthlyPrice() {
        return MONTHLY_PRICE;
    }

    public String currency() {
        return CURRENCY;
    }
}
