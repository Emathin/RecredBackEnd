package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetStudentWalletSummaryResponse;
import com.recred.backend.domain.models.WalletHealthSpending;
import com.recred.backend.infrastructure.web.WalletHealthSpendingResponseDTO;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletHealthSpendingResponseDTOTest {

    private WalletHealthSpendingResponseDTO result;

    @Test
    void givenDomainModelWhenMappingFromDomainThenReturnsMappedHealthSpending() {
        WalletHealthSpending model = givenDomainModel(
                "Sin TACC", "250.00", "20.00");

        result = whenMappingFromDomain(model);

        thenClasificacionIs("Sin TACC");
        thenMontoIs("250.00");
        thenPorcentajeIs("20.00");
    }

    @Test
    void givenApplicationModelWhenMappingFromApplicationThenReturnsMappedHealthSpending() {
        GetStudentWalletSummaryResponse.HealthSpending model =
                givenApplicationModel("Vegano", "500.00", "40.00");

        result = whenMappingFromApplication(model);

        thenClasificacionIs("Vegano");
        thenMontoIs("500.00");
        thenPorcentajeIs("40.00");
    }

    private WalletHealthSpending givenDomainModel(
            String clasificacion, String monto, String porcentaje) {
        return new WalletHealthSpending(
                clasificacion, new BigDecimal(monto), new BigDecimal(porcentaje));
    }

    private GetStudentWalletSummaryResponse.HealthSpending givenApplicationModel(
            String clasificacion, String monto, String porcentaje) {
        return new GetStudentWalletSummaryResponse.HealthSpending(
                clasificacion, new BigDecimal(monto), new BigDecimal(porcentaje));
    }

    private WalletHealthSpendingResponseDTO whenMappingFromDomain(
            WalletHealthSpending model) {
        return WalletHealthSpendingResponseDTO.fromDomain(model);
    }

    private WalletHealthSpendingResponseDTO whenMappingFromApplication(
            GetStudentWalletSummaryResponse.HealthSpending model) {
        return WalletHealthSpendingResponseDTO.fromApplication(model);
    }

    private void thenClasificacionIs(String expected) {
        assertEquals(expected, result.clasificacion());
    }

    private void thenMontoIs(String expected) {
        assertEquals(new BigDecimal(expected), result.monto());
    }

    private void thenPorcentajeIs(String expected) {
        assertEquals(new BigDecimal(expected), result.porcentaje());
    }
}
