package com.recred.backend.unit.application;

import com.recred.backend.application.ProductSaleStatisticsResponse;
import com.recred.backend.domain.models.ProductSaleStatistics;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductSaleStatisticsResponseTest {

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final String PRODUCT_NAME = "Alfajor";
    private static final String CATEGORY = "Golosinas";
    private static final BigDecimal CURRENT_PRICE = new BigDecimal("550.00");
    private static final Long SALES_PERIOD = 18L;
    private static final BigDecimal SALES_PARTICIPATION = new BigDecimal("22.50");
    private static final Integer GENERAL_RANKING = 3;
    private static final Integer CATEGORY_RANKING = 1;
    private static final BigDecimal AVG_CATEGORY_SALES = new BigDecimal("14.00");
    private static final BigDecimal AVG_CATEGORY_PRICE = new BigDecimal("500.00");
    private static final BigDecimal PRICE_DIFFERENCE = new BigDecimal("50.00");
    private static final Long DAYS_WITHOUT_SALE = 2L;
    private static final Long DISTINCT_CLIENTS = 8L;
    private static final Integer CURRENT_STOCK = 20;
    private static final BigDecimal AVG_CATEGORY_STOCK = new BigDecimal("18.50");

    @Test
    void fromDomain_ShouldMapAllStatisticsFields() {
        ProductSaleStatistics statistics = givenProductSaleStatistics();
        ProductSaleStatisticsResponse response = whenFromDomain(statistics);
        thenAllFieldsAreMappedCorrectly(response, statistics);
    }

    private ProductSaleStatistics givenProductSaleStatistics() {
        return ProductSaleStatistics.builder()
                .productoId(PRODUCT_ID)
                .nombre(PRODUCT_NAME)
                .categoria(CATEGORY)
                .precioActual(CURRENT_PRICE)
                .ventasPeriodo(SALES_PERIOD)
                .participacionVentas(SALES_PARTICIPATION)
                .rankingGeneral(GENERAL_RANKING)
                .rankingCategoria(CATEGORY_RANKING)
                .promedioVentasCategoria(AVG_CATEGORY_SALES)
                .promedioPrecioCategoria(AVG_CATEGORY_PRICE)
                .diferenciaPrecioCategoria(PRICE_DIFFERENCE)
                .diasSinVenta(DAYS_WITHOUT_SALE)
                .clientesDistintos(DISTINCT_CLIENTS)
                .stockActual(CURRENT_STOCK)
                .stockPromedioCategoria(AVG_CATEGORY_STOCK)
                .build();
    }

    private ProductSaleStatisticsResponse whenFromDomain(ProductSaleStatistics statistics) {
        return ProductSaleStatisticsResponse.fromDomain(statistics);
    }

    private void thenAllFieldsAreMappedCorrectly(
            ProductSaleStatisticsResponse response, ProductSaleStatistics statistics) {
        assertEquals(statistics.getProductoId(), response.productoId());
        assertEquals(statistics.getNombre(), response.nombre());
        assertEquals(statistics.getCategoria(), response.categoria());
        assertEquals(statistics.getPrecioActual(), response.precioActual());
        assertEquals(statistics.getVentasPeriodo(), response.ventasPeriodo());
        assertEquals(statistics.getParticipacionVentas(), response.participacionVentas());
        assertEquals(statistics.getRankingGeneral(), response.rankingGeneral());
        assertEquals(statistics.getRankingCategoria(), response.rankingCategoria());
        assertEquals(statistics.getPromedioVentasCategoria(), response.promedioVentasCategoria());
        assertEquals(statistics.getPromedioPrecioCategoria(), response.promedioPrecioCategoria());
        assertEquals(statistics.getDiferenciaPrecioCategoria(), response.diferenciaPrecioCategoria());
        assertEquals(statistics.getDiasSinVenta(), response.diasSinVenta());
        assertEquals(statistics.getClientesDistintos(), response.clientesDistintos());
        assertEquals(statistics.getStockActual(), response.stockActual());
        assertEquals(statistics.getStockPromedioCategoria(), response.stockPromedioCategoria());
    }
}
