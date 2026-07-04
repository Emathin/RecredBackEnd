package com.recred.backend.unit.application;

import com.recred.backend.application.ProductSaleStatisticsResponse;
import com.recred.backend.domain.models.ProductSaleStatistics;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductSaleStatisticsResponseTest {

    @Test
    void fromDomainShouldMapAllStatisticsFields() {
        UUID productId = UUID.randomUUID();
        ProductSaleStatistics statistics = ProductSaleStatistics.builder()
                .productoId(productId)
                .nombre("Alfajor")
                .categoria("Golosinas")
                .precioActual(new BigDecimal("550.00"))
                .ventasPeriodo(18L)
                .participacionVentas(new BigDecimal("22.50"))
                .rankingGeneral(3)
                .rankingCategoria(1)
                .promedioVentasCategoria(new BigDecimal("14.00"))
                .promedioPrecioCategoria(new BigDecimal("500.00"))
                .diferenciaPrecioCategoria(new BigDecimal("50.00"))
                .diasSinVenta(2L)
                .clientesDistintos(8L)
                .stockActual(20)
                .stockPromedioCategoria(new BigDecimal("18.50"))
                .build();

        ProductSaleStatisticsResponse response =
                ProductSaleStatisticsResponse.fromDomain(statistics);

        assertEquals(productId, response.productoId());
        assertEquals("Alfajor", response.nombre());
        assertEquals("Golosinas", response.categoria());
        assertEquals(new BigDecimal("550.00"), response.precioActual());
        assertEquals(18L, response.ventasPeriodo());
        assertEquals(new BigDecimal("22.50"), response.participacionVentas());
        assertEquals(3, response.rankingGeneral());
        assertEquals(1, response.rankingCategoria());
        assertEquals(new BigDecimal("14.00"), response.promedioVentasCategoria());
        assertEquals(new BigDecimal("500.00"), response.promedioPrecioCategoria());
        assertEquals(new BigDecimal("50.00"), response.diferenciaPrecioCategoria());
        assertEquals(2L, response.diasSinVenta());
        assertEquals(8L, response.clientesDistintos());
        assertEquals(20, response.stockActual());
        assertEquals(new BigDecimal("18.50"), response.stockPromedioCategoria());
    }
}
