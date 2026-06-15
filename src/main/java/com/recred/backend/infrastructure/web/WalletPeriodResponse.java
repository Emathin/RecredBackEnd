package com.recred.backend.infrastructure.web;

import java.time.LocalDate;

public record WalletPeriodResponse(
        LocalDate desde,
        LocalDate hasta
) {
}
