package com.recred.backend.unit.application;

import com.recred.backend.application.*;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.infrastructure.persistence.ProductStatisticsJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProductStatisticsTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductStatisticsJpaRepository productStatisticsJpaRepository;

    @Mock
    private KiosqueroRepository kiosqueroRepository;

    private GetProductStatistics getProductStatistics;

    @BeforeEach
    void setUp() {
        getProductStatistics = new GetProductStatistics(
                productRepository,
                productStatisticsJpaRepository,
                kiosqueroRepository
        );
    }

    @Test
    void executeShouldBuildStatisticsWithSalesAndRankings() {
        UUID productId = UUID.randomUUID();
        UUID kiosqueroId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Producto product = product(productId, categoryId);
        UUID otherProductId = UUID.randomUUID();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(kiosqueroRepository.findBuffetByUsuarioId(kiosqueroId)).thenReturn(buffetId);
        when(productStatisticsJpaRepository.countTotalSalesByProductAndBuffet(
                eq(productId),
                eq(buffetId),
                any()
        )).thenReturn(12L);
        when(productStatisticsJpaRepository.countDistinctCustomersByProductAndBuffet(
                eq(productId),
                eq(buffetId),
                any()
        )).thenReturn(5L);
        when(productStatisticsJpaRepository.getSalesDatesByProduct(
                eq(productId),
                eq(buffetId),
                any()
        )).thenReturn(List.of(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 2)
        ));
        when(productStatisticsJpaRepository.getSalesRankingsGeneral(eq(buffetId), any()))
                .thenReturn(List.<Object[]>of(
                        new Object[] {otherProductId, 20L},
                        new Object[] {productId, 12L}
                ));
        when(productStatisticsJpaRepository.getSalesRankingsByCategory(
                eq(buffetId),
                eq(categoryId),
                any()
        )).thenReturn(List.<Object[]>of(new Object[] {productId, 12L}));
        when(productStatisticsJpaRepository.getStockActual(productId, buffetId)).thenReturn(8);
        when(productStatisticsJpaRepository.getAveragePriceByCategory(categoryId))
                .thenReturn(new BigDecimal("30.00"));
        when(productStatisticsJpaRepository.getAverageSalesVolumeByCategory(
                eq(buffetId),
                eq(categoryId),
                any()
        )).thenReturn(new BigDecimal("6.50"));
        when(productStatisticsJpaRepository.getAverageStockByCategory(categoryId, buffetId))
                .thenReturn(new BigDecimal("10.00"));

        ProductSaleStatisticsResponse result = getProductStatistics.execute(
                new GetProductStatisticsRequest(productId, kiosqueroId));

        assertEquals(productId, result.productoId());
        assertEquals("Alfajor", result.nombre());
        assertEquals("Golosinas", result.categoria());
        assertEquals(new BigDecimal("45.00"), result.precioActual());
        assertEquals(12L, result.ventasPeriodo());
        assertEquals(13L, result.diasSinVenta());
        assertEquals(new BigDecimal("37.5000"), result.participacionVentas());
        assertEquals(2, result.rankingGeneral());
        assertEquals(1, result.rankingCategoria());
        assertEquals(5L, result.clientesDistintos());
        assertEquals(8, result.stockActual());
        assertEquals(new BigDecimal("30.00"), result.promedioPrecioCategoria());
        assertEquals(new BigDecimal("6.50"), result.promedioVentasCategoria());
        assertEquals(new BigDecimal("10.00"), result.stockPromedioCategoria());
        assertEquals(new BigDecimal("15.00"), result.diferenciaPrecioCategoria());
    }

    @Test
    void executeShouldUseDefaultsWhenSalesAreMissing() {
        UUID productId = UUID.randomUUID();
        UUID kiosqueroId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Producto product = product(productId, categoryId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(kiosqueroRepository.findBuffetByUsuarioId(kiosqueroId)).thenReturn(buffetId);
        when(productStatisticsJpaRepository.countTotalSalesByProductAndBuffet(
                eq(productId),
                eq(buffetId),
                any()
        )).thenReturn(null);
        when(productStatisticsJpaRepository.countDistinctCustomersByProductAndBuffet(
                eq(productId),
                eq(buffetId),
                any()
        )).thenReturn(null);
        when(productStatisticsJpaRepository.getSalesDatesByProduct(
                eq(productId),
                eq(buffetId),
                any()
        )).thenReturn(List.of());
        when(productStatisticsJpaRepository.getSalesRankingsGeneral(eq(buffetId), any()))
                .thenReturn(List.of());
        when(productStatisticsJpaRepository.getSalesRankingsByCategory(
                eq(buffetId),
                eq(categoryId),
                any()
        )).thenReturn(List.of());
        when(productStatisticsJpaRepository.getStockActual(productId, buffetId)).thenReturn(null);
        when(productStatisticsJpaRepository.getAveragePriceByCategory(categoryId)).thenReturn(null);
        when(productStatisticsJpaRepository.getAverageSalesVolumeByCategory(
                eq(buffetId),
                eq(categoryId),
                any()
        )).thenReturn(null);
        when(productStatisticsJpaRepository.getAverageStockByCategory(categoryId, buffetId))
                .thenReturn(null);

        ProductSaleStatisticsResponse result = getProductStatistics.execute(
                new GetProductStatisticsRequest(productId, kiosqueroId));

        assertEquals(0L, result.ventasPeriodo());
        assertEquals(15L, result.diasSinVenta());
        assertEquals(BigDecimal.ZERO, result.participacionVentas());
        assertEquals(0, result.rankingGeneral());
        assertEquals(0, result.rankingCategoria());
        assertEquals(0L, result.clientesDistintos());
        assertEquals(0, result.stockActual());
        assertEquals(BigDecimal.ZERO, result.diferenciaPrecioCategoria());
    }

    @Test
    void executeShouldThrowWhenProductDoesNotExist() {
        UUID productId = UUID.randomUUID();
        UUID kiosqueroId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                getProductStatistics.execute(
                        new GetProductStatisticsRequest(productId, kiosqueroId)));
        verify(kiosqueroRepository, never()).findBuffetByUsuarioId(any());
    }

    @Test
    void executeShouldThrowWhenKiosqueroHasNoBuffet() {
        UUID productId = UUID.randomUUID();
        UUID kiosqueroId = UUID.randomUUID();
        Producto product = product(productId, UUID.randomUUID());

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(kiosqueroRepository.findBuffetByUsuarioId(kiosqueroId)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () ->
                getProductStatistics.execute(
                        new GetProductStatisticsRequest(productId, kiosqueroId)));
        verify(productStatisticsJpaRepository, never()).countTotalSalesByProductAndBuffet(
                any(),
                any(),
                any()
        );
    }

    private Producto product(UUID productId, UUID categoryId) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(categoryId);
        category.setDescripcion("Golosinas");

        Producto product = new Producto();
        product.setId(productId);
        product.setNombre("Alfajor");
        product.setPrecio(new BigDecimal("45.00"));
        product.setCategoria(category);
        return product;
    }
}
