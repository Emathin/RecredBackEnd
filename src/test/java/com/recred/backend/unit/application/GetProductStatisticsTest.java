package com.recred.backend.unit.application;

import com.recred.backend.application.*;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ProductSaleStatistics;
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

        ProductSaleStatistics result = getProductStatistics.execute(productId, kiosqueroId);

        assertEquals(productId, result.getProductoId());
        assertEquals("Alfajor", result.getNombre());
        assertEquals("Golosinas", result.getCategoria());
        assertEquals(new BigDecimal("45.00"), result.getPrecioActual());
        assertEquals(12L, result.getVentasPeriodo());
        assertEquals(13L, result.getDiasSinVenta());
        assertEquals(new BigDecimal("37.5000"), result.getParticipacionVentas());
        assertEquals(2, result.getRankingGeneral());
        assertEquals(1, result.getRankingCategoria());
        assertEquals(5L, result.getClientesDistintos());
        assertEquals(8, result.getStockActual());
        assertEquals(new BigDecimal("30.00"), result.getPromedioPrecioCategoria());
        assertEquals(new BigDecimal("6.50"), result.getPromedioVentasCategoria());
        assertEquals(new BigDecimal("10.00"), result.getStockPromedioCategoria());
        assertEquals(new BigDecimal("15.00"), result.getDiferenciaPrecioCategoria());
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

        ProductSaleStatistics result = getProductStatistics.execute(productId, kiosqueroId);

        assertEquals(0L, result.getVentasPeriodo());
        assertEquals(15L, result.getDiasSinVenta());
        assertEquals(BigDecimal.ZERO, result.getParticipacionVentas());
        assertEquals(0, result.getRankingGeneral());
        assertEquals(0, result.getRankingCategoria());
        assertEquals(0L, result.getClientesDistintos());
        assertEquals(0, result.getStockActual());
        assertEquals(BigDecimal.ZERO, result.getDiferenciaPrecioCategoria());
    }

    @Test
    void executeShouldThrowWhenProductDoesNotExist() {
        UUID productId = UUID.randomUUID();
        UUID kiosqueroId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                getProductStatistics.execute(productId, kiosqueroId));
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
                getProductStatistics.execute(productId, kiosqueroId));
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
