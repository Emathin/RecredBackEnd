package com.recred.backend.application;

import com.recred.backend.domain.models.ProductSaleStatistics;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.infrastructure.persistence.ProductStatisticsJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
public class GetProductStatistics {

    private static final int STATISTICS_PERIOD_DAYS = 15;
    private static final int SALES_PARTICIPATION_SCALE = 4;
    private static final BigDecimal PERCENTAGE_FACTOR = BigDecimal.valueOf(100);

    private final ProductRepository productRepository;
    private final ProductStatisticsJpaRepository productStatisticsJpaRepository;
    private final KiosqueroRepository kiosqueroRepository;

    public GetProductStatistics(
            ProductRepository productRepository,
            ProductStatisticsJpaRepository productStatisticsJpaRepository,
            KiosqueroRepository kiosqueroRepository
    ) {
        this.productRepository = productRepository;
        this.productStatisticsJpaRepository = productStatisticsJpaRepository;
        this.kiosqueroRepository = kiosqueroRepository;
    }

    @Transactional(readOnly = true)
    public ProductSaleStatisticsResponse execute(
            GetProductStatisticsRequest statisticsRequest) {
        return ProductSaleStatisticsResponse.fromDomain(statisticsFor(statisticsRequest));
    }

    private ProductSaleStatistics statisticsFor(GetProductStatisticsRequest statisticsRequest) {
        Producto product = findProduct(statisticsRequest.productId());
        UUID buffetId = findBuffetForKiosquero(statisticsRequest.kiosqueroId());
        LocalDateTime since = LocalDateTime.now().minusDays(STATISTICS_PERIOD_DAYS);

        Long sales = countProductSales(statisticsRequest.productId(), buffetId, since);
        Long customerCount = countProductCustomers(
                statisticsRequest.productId(),
                buffetId,
                since);
        List<LocalDate> salesDates = salesDatesFor(
                statisticsRequest.productId(),
                buffetId,
                since);
        List<Object[]> generalRankings = generalSalesRankings(buffetId, since);
        List<Object[]> categoryRankings = categorySalesRankings(product, buffetId, since);
        Integer stock = currentStock(statisticsRequest.productId(), buffetId);
        BigDecimal averageCategoryPrice = averageCategoryPrice(product);
        BigDecimal averageCategorySales = averageCategorySales(product, buffetId, since);
        BigDecimal averageCategoryStock = averageCategoryStock(product, buffetId);

        return ProductSaleStatistics.builder()
                .productoId(product.getId())
                .nombre(product.getNombre())
                .categoria(product.getCategoria().getDescripcion())
                .precioActual(product.getPrecio())
                .ventasPeriodo(countOrZero(sales))
                .diasSinVenta(daysWithoutSales(salesDates))
                .participacionVentas(salesParticipation(sales, generalRankings))
                .rankingGeneral(rankingFor(statisticsRequest.productId(), generalRankings))
                .rankingCategoria(rankingFor(statisticsRequest.productId(), categoryRankings))
                .clientesDistintos(countOrZero(customerCount))
                .stockActual(stockOrZero(stock))
                .promedioPrecioCategoria(averageCategoryPrice)
                .promedioVentasCategoria(averageCategorySales)
                .stockPromedioCategoria(averageCategoryStock)
                .diferenciaPrecioCategoria(priceDifferenceFromCategoryAverage(
                        product,
                        averageCategoryPrice))
                .build();
    }

    private Producto findProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
    }

    private UUID findBuffetForKiosquero(UUID kiosqueroId) {
        UUID buffetId = kiosqueroRepository.findBuffetByUsuarioId(kiosqueroId);
        if (buffetId == null) {
            throw new IllegalArgumentException("Kiosquero no encontrado o sin buffet asociado");
        }
        return buffetId;
    }

    private Long countProductSales(UUID productId, UUID buffetId, LocalDateTime since) {
        return productStatisticsJpaRepository
                .countTotalSalesByProductAndBuffet(productId, buffetId, since);
    }

    private Long countProductCustomers(UUID productId, UUID buffetId, LocalDateTime since) {
        return productStatisticsJpaRepository
                .countDistinctCustomersByProductAndBuffet(productId, buffetId, since);
    }

    private List<LocalDate> salesDatesFor(UUID productId, UUID buffetId, LocalDateTime since) {
        return productStatisticsJpaRepository.getSalesDatesByProduct(productId, buffetId, since);
    }

    private List<Object[]> generalSalesRankings(UUID buffetId, LocalDateTime since) {
        return productStatisticsJpaRepository.getSalesRankingsGeneral(buffetId, since);
    }

    private List<Object[]> categorySalesRankings(
            Producto product,
            UUID buffetId,
            LocalDateTime since
    ) {
        return productStatisticsJpaRepository.getSalesRankingsByCategory(
                buffetId,
                product.getCategoria().getId(),
                since);
    }

    private Integer currentStock(UUID productId, UUID buffetId) {
        return productStatisticsJpaRepository.getStockActual(productId, buffetId);
    }

    private BigDecimal averageCategoryPrice(Producto product) {
        return productStatisticsJpaRepository
                .getAveragePriceByCategory(product.getCategoria().getId());
    }

    private BigDecimal averageCategorySales(
            Producto product,
            UUID buffetId,
            LocalDateTime since
    ) {
        return productStatisticsJpaRepository.getAverageSalesVolumeByCategory(
                buffetId,
                product.getCategoria().getId(),
                since);
    }

    private BigDecimal averageCategoryStock(Producto product, UUID buffetId) {
        return productStatisticsJpaRepository.getAverageStockByCategory(
                product.getCategoria().getId(),
                buffetId);
    }

    private long daysWithoutSales(List<LocalDate> salesDates) {
        long daysWithSales = salesDates.stream().distinct().count();
        return STATISTICS_PERIOD_DAYS - daysWithSales;
    }

    private BigDecimal salesParticipation(Long productSales, List<Object[]> generalRankings) {
        long totalBuffetSales = totalSales(generalRankings);
        if (totalBuffetSales == 0 || productSales == null) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(productSales)
                .divide(
                        BigDecimal.valueOf(totalBuffetSales),
                        SALES_PARTICIPATION_SCALE,
                        RoundingMode.HALF_UP)
                .multiply(PERCENTAGE_FACTOR);
    }

    private long totalSales(List<Object[]> rankings) {
        return rankings.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();
    }

    private int rankingFor(UUID productId, List<Object[]> rankings) {
        return IntStream.range(0, rankings.size())
                .filter(i -> ((UUID) rankings.get(i)[0]).equals(productId))
                .findFirst()
                .orElse(-1) + 1;
    }

    private long countOrZero(Long count) {
        return count == null ? 0L : count;
    }

    private int stockOrZero(Integer stock) {
        return stock == null ? 0 : stock;
    }

    private BigDecimal priceDifferenceFromCategoryAverage(
            Producto product,
            BigDecimal averageCategoryPrice
    ) {
        if (averageCategoryPrice == null) {
            return BigDecimal.ZERO;
        }

        return product.getPrecio().subtract(averageCategoryPrice);
    }
}
