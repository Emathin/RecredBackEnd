package com.recred.backend.unit.application;

import com.recred.backend.application.GetProductStatistics;
import com.recred.backend.application.GetProductStatisticsRequest;
import com.recred.backend.application.ProductSaleStatisticsResponse;
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

    private GetProductStatistics useCase;

    private UUID productId;
    private UUID kiosqueroId;
    private UUID buffetId;
    private UUID categoryId;
    private Producto product;
    private ProductSaleStatisticsResponse response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new GetProductStatistics(
                productRepository,
                productStatisticsJpaRepository,
                kiosqueroRepository
        );
    }

    @Test
    void givenProductSalesDuringPeriod_whenGettingProductStatistics_thenReturnsSalesAndRankings() {
        givenProductStatisticsScenario();
        givenProductExists();
        givenKiosqueroHasBuffet();
        givenProductHasSalesDuringPeriod();

        whenGettingProductStatistics();

        thenProductStatisticsAreReturned();
        thenSalesStatisticsAreReturned();
        thenCategoryComparisonsAreReturned();
    }

    @Test
    void givenProductWithoutSalesDuringPeriod_whenGettingProductStatistics_thenReturnsDefaults() {
        givenProductStatisticsScenario();
        givenProductExists();
        givenKiosqueroHasBuffet();
        givenProductHasNoSalesDuringPeriod();

        whenGettingProductStatistics();

        thenDefaultSalesStatisticsAreReturned();
    }

    @Test
    void givenMissingProduct_whenGettingProductStatistics_thenFailsBeforeLoadingBuffet() {
        givenProductStatisticsScenario();
        givenProductDoesNotExist();

        whenGettingProductStatisticsFails();

        thenProductStatisticsFailWith("Producto no encontrado");
        thenBuffetWasNotRequested();
    }

    @Test
    void givenKiosqueroWithoutBuffet_whenGettingProductStatistics_thenFailsBeforeCountingSales() {
        givenProductStatisticsScenario();
        givenProductExists();
        givenKiosqueroDoesNotHaveBuffet();

        whenGettingProductStatisticsFails();

        thenProductStatisticsFailWith("Kiosquero no encontrado o sin buffet asociado");
        thenSalesWereNotCounted();
    }

    private void givenProductStatisticsScenario() {
        productId = UUID.randomUUID();
        kiosqueroId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        product = product(productId, categoryId);
    }

    private void givenProductExists() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    }

    private void givenProductDoesNotExist() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
    }

    private void givenKiosqueroHasBuffet() {
        when(kiosqueroRepository.findBuffetByUsuarioId(kiosqueroId)).thenReturn(buffetId);
    }

    private void givenKiosqueroDoesNotHaveBuffet() {
        when(kiosqueroRepository.findBuffetByUsuarioId(kiosqueroId)).thenReturn(null);
    }

    private void givenProductHasSalesDuringPeriod() {
        UUID otherProductId = UUID.randomUUID();
        givenSalesCounters(12L, 5L);
        givenSalesDates(List.of(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 2)
        ));
        givenGeneralRankings(List.<Object[]>of(
                new Object[] {otherProductId, 20L},
                new Object[] {productId, 12L}
        ));
        givenCategoryRankings(List.<Object[]>of(new Object[] {productId, 12L}));
        givenCurrentStock(8);
        givenCategoryAverages(
                new BigDecimal("30.00"),
                new BigDecimal("6.50"),
                new BigDecimal("10.00")
        );
    }

    private void givenProductHasNoSalesDuringPeriod() {
        givenSalesCounters(null, null);
        givenSalesDates(List.of());
        givenGeneralRankings(List.of());
        givenCategoryRankings(List.of());
        givenCurrentStock(null);
        givenCategoryAverages(null, null, null);
    }

    private void givenSalesCounters(Long sales, Long customers) {
        when(productStatisticsJpaRepository.countTotalSalesByProductAndBuffet(
                eq(productId),
                eq(buffetId),
                any()
        )).thenReturn(sales);
        when(productStatisticsJpaRepository.countDistinctCustomersByProductAndBuffet(
                eq(productId),
                eq(buffetId),
                any()
        )).thenReturn(customers);
    }

    private void givenSalesDates(List<LocalDate> salesDates) {
        when(productStatisticsJpaRepository.getSalesDatesByProduct(
                eq(productId),
                eq(buffetId),
                any()
        )).thenReturn(salesDates);
    }

    private void givenGeneralRankings(List<Object[]> rankings) {
        when(productStatisticsJpaRepository.getSalesRankingsGeneral(eq(buffetId), any()))
                .thenReturn(rankings);
    }

    private void givenCategoryRankings(List<Object[]> rankings) {
        when(productStatisticsJpaRepository.getSalesRankingsByCategory(
                eq(buffetId),
                eq(categoryId),
                any()
        )).thenReturn(rankings);
    }

    private void givenCurrentStock(Integer stock) {
        when(productStatisticsJpaRepository.getStockActual(productId, buffetId))
                .thenReturn(stock);
    }

    private void givenCategoryAverages(
            BigDecimal averagePrice,
            BigDecimal averageSales,
            BigDecimal averageStock
    ) {
        when(productStatisticsJpaRepository.getAveragePriceByCategory(categoryId))
                .thenReturn(averagePrice);
        when(productStatisticsJpaRepository.getAverageSalesVolumeByCategory(
                eq(buffetId),
                eq(categoryId),
                any()
        )).thenReturn(averageSales);
        when(productStatisticsJpaRepository.getAverageStockByCategory(categoryId, buffetId))
                .thenReturn(averageStock);
    }

    private void whenGettingProductStatistics() {
        response = useCase.execute(new GetProductStatisticsRequest(productId, kiosqueroId));
    }

    private void whenGettingProductStatisticsFails() {
        thrownException = assertThrows(RuntimeException.class, () ->
                useCase.execute(new GetProductStatisticsRequest(productId, kiosqueroId)));
    }

    private void thenProductStatisticsAreReturned() {
        assertEquals(productId, response.productoId());
        assertEquals("Alfajor", response.nombre());
        assertEquals("Golosinas", response.categoria());
        assertEquals(new BigDecimal("45.00"), response.precioActual());
    }

    private void thenSalesStatisticsAreReturned() {
        assertEquals(12L, response.ventasPeriodo());
        assertEquals(13L, response.diasSinVenta());
        assertEquals(new BigDecimal("37.5000"), response.participacionVentas());
        assertEquals(2, response.rankingGeneral());
        assertEquals(1, response.rankingCategoria());
        assertEquals(5L, response.clientesDistintos());
        assertEquals(8, response.stockActual());
    }

    private void thenCategoryComparisonsAreReturned() {
        assertEquals(new BigDecimal("30.00"), response.promedioPrecioCategoria());
        assertEquals(new BigDecimal("6.50"), response.promedioVentasCategoria());
        assertEquals(new BigDecimal("10.00"), response.stockPromedioCategoria());
        assertEquals(new BigDecimal("15.00"), response.diferenciaPrecioCategoria());
    }

    private void thenDefaultSalesStatisticsAreReturned() {
        assertEquals(0L, response.ventasPeriodo());
        assertEquals(15L, response.diasSinVenta());
        assertEquals(BigDecimal.ZERO, response.participacionVentas());
        assertEquals(0, response.rankingGeneral());
        assertEquals(0, response.rankingCategoria());
        assertEquals(0L, response.clientesDistintos());
        assertEquals(0, response.stockActual());
        assertEquals(BigDecimal.ZERO, response.diferenciaPrecioCategoria());
    }

    private void thenProductStatisticsFailWith(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private void thenBuffetWasNotRequested() {
        verify(kiosqueroRepository, never()).findBuffetByUsuarioId(any());
    }

    private void thenSalesWereNotCounted() {
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
