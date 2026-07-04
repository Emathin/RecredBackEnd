package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetStudentWalletSummaryResponse;
import com.recred.backend.domain.models.WalletHealthSpending;
import com.recred.backend.infrastructure.web.WalletHealthSpendingResponseDTO;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletHealthSpendingResponseDTOTest {

    @Test
    void fromDomainShouldMapHealthSpending() {
        WalletHealthSpendingResponseDTO dto =
                WalletHealthSpendingResponseDTO.fromDomain(
                        new WalletHealthSpending(
                                "Sin TACC",
                                new BigDecimal("250.00"),
                                new BigDecimal("20.00")
                        )
                );

        assertEquals("Sin TACC", dto.clasificacion());
        assertEquals(new BigDecimal("250.00"), dto.monto());
        assertEquals(new BigDecimal("20.00"), dto.porcentaje());
    }

    @Test
    void fromApplicationShouldMapHealthSpending() {
        WalletHealthSpendingResponseDTO dto =
                WalletHealthSpendingResponseDTO.fromApplication(
                        new GetStudentWalletSummaryResponse.HealthSpending(
                                "Vegano",
                                new BigDecimal("500.00"),
                                new BigDecimal("40.00")
                        )
                );

        assertEquals("Vegano", dto.clasificacion());
        assertEquals(new BigDecimal("500.00"), dto.monto());
        assertEquals(new BigDecimal("40.00"), dto.porcentaje());
    }
}
