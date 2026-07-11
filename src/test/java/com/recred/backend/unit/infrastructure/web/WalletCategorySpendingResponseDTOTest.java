package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetStudentWalletSummaryResponse;
import com.recred.backend.domain.models.WalletCategorySpending;
import com.recred.backend.infrastructure.web.WalletCategorySpendingResponseDTO;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletCategorySpendingResponseDTOTest {

    private WalletCategorySpendingResponseDTO result;

    @Test
    void givenDomainModelWhenMappingFromDomainThenReturnsMappedCategorySpending() {
        WalletCategorySpending model = givenDomainModel(
                "Bebidas", "300.00", "25.00");

        result = whenMappingFromDomain(model);

        thenCategoriaIs("Bebidas");
        thenMontoIs("300.00");
        thenPorcentajeIs("25.00");
    }

    @Test
    void givenApplicationModelWhenMappingFromApplicationThenReturnsMappedCategorySpending() {
        GetStudentWalletSummaryResponse.CategorySpending model =
                givenApplicationModel("Snacks", "900.00", "75.00");

        result = whenMappingFromApplication(model);

        thenCategoriaIs("Snacks");
        thenMontoIs("900.00");
        thenPorcentajeIs("75.00");
    }

    private WalletCategorySpending givenDomainModel(
            String categoria, String monto, String porcentaje) {
        return new WalletCategorySpending(
                categoria, new BigDecimal(monto), new BigDecimal(porcentaje));
    }

    private GetStudentWalletSummaryResponse.CategorySpending givenApplicationModel(
            String categoria, String monto, String porcentaje) {
        return new GetStudentWalletSummaryResponse.CategorySpending(
                categoria, new BigDecimal(monto), new BigDecimal(porcentaje));
    }

    private WalletCategorySpendingResponseDTO whenMappingFromDomain(
            WalletCategorySpending model) {
        return WalletCategorySpendingResponseDTO.fromDomain(model);
    }

    private WalletCategorySpendingResponseDTO whenMappingFromApplication(
            GetStudentWalletSummaryResponse.CategorySpending model) {
        return WalletCategorySpendingResponseDTO.fromApplication(model);
    }

    private void thenCategoriaIs(String expected) {
        assertEquals(expected, result.categoria());
    }

    private void thenMontoIs(String expected) {
        assertEquals(new BigDecimal(expected), result.monto());
    }

    private void thenPorcentajeIs(String expected) {
        assertEquals(new BigDecimal(expected), result.porcentaje());
    }
}
