package com.recred.backend.infrastructure.web;

import java.time.LocalDate;

public record WalletPeriodResponseDTO(
        LocalDate desde,
        LocalDate hasta
) {
}
