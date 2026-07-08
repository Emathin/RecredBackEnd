package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetStudentWalletSummaryResponse;
import com.recred.backend.domain.models.WalletCategorySpending;
import com.recred.backend.infrastructure.web.WalletCategorySpendingResponseDTO;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletCategorySpendingResponseDTOTest {

    @Test
    void fromDomainShouldMapCategorySpending() {
        WalletCategorySpendingResponseDTO dto =
                WalletCategorySpendingResponseDTO.fromDomain(
                        new WalletCategorySpending(
                                "Bebidas",
                                new BigDecimal("300.00"),
                                new BigDecimal("25.00")
                        )
                );

        assertEquals("Bebidas", dto.categoria());
        assertEquals(new BigDecimal("300.00"), dto.monto());
        assertEquals(new BigDecimal("25.00"), dto.porcentaje());
    }

    @Test
    void fromApplicationShouldMapCategorySpending() {
        WalletCategorySpendingResponseDTO dto =
                WalletCategorySpendingResponseDTO.fromApplication(
                        new GetStudentWalletSummaryResponse.CategorySpending(
                                "Snacks",
                                new BigDecimal("900.00"),
                                new BigDecimal("75.00")
                        )
                );

        assertEquals("Snacks", dto.categoria());
        assertEquals(new BigDecimal("900.00"), dto.monto());
        assertEquals(new BigDecimal("75.00"), dto.porcentaje());
    }
}
