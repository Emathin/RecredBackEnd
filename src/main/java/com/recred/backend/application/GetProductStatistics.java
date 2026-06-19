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
    public ProductSaleStatistics execute(UUID productId, UUID kiosqueroId) {
        Producto producto = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        
        UUID buffetId = kiosqueroRepository.findBuffetByUsuarioId(kiosqueroId);
        if (buffetId == null) {
            throw new IllegalArgumentException("Kiosquero no encontrado o sin buffet asociado");
        }
        LocalDateTime since = LocalDateTime.now().minusDays(15);

        Long ventas = productStatisticsJpaRepository
                .countTotalSalesByProductAndBuffet(productId, buffetId, since);
        Long clientes = productStatisticsJpaRepository
                .countDistinctCustomersByProductAndBuffet(productId, buffetId, since);
        
        List<LocalDate> salesDates = productStatisticsJpaRepository
                .getSalesDatesByProduct(productId, buffetId, since);
        long diasConVentas = salesDates.stream().distinct().count();
        long diasSinVenta = 15 - diasConVentas;

        List<Object[]> generalRankings = productStatisticsJpaRepository
                .getSalesRankingsGeneral(buffetId, since);
        long totalBuffetSales = generalRankings.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();

        BigDecimal participacionVentas = (totalBuffetSales > 0 && ventas != null) ?
                BigDecimal.valueOf(ventas)
                        .divide(BigDecimal.valueOf(totalBuffetSales), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

        int rankingGeneral = IntStream.range(0, generalRankings.size())
                .filter(i -> ((UUID) generalRankings.get(i)[0]).equals(productId))
                .findFirst().orElse(-1) + 1;

        List<Object[]> catRankings = productStatisticsJpaRepository
                .getSalesRankingsByCategory(buffetId, producto.getCategoria().getId(), since);
        int rankingCat = IntStream.range(0, catRankings.size())
                .filter(i -> ((UUID) catRankings.get(i)[0]).equals(productId))
                .findFirst().orElse(-1) + 1;
        
        Integer stock = productStatisticsJpaRepository.getStockActual(productId, buffetId);
        BigDecimal avgPriceCat = productStatisticsJpaRepository
                .getAveragePriceByCategory(producto.getCategoria().getId());
        BigDecimal avgSalesCat = productStatisticsJpaRepository
                .getAverageSalesVolumeByCategory(buffetId, producto.getCategoria().getId(), since);
        BigDecimal avgStockCat = productStatisticsJpaRepository
                .getAverageStockByCategory(producto.getCategoria().getId(), buffetId);

        return ProductSaleStatistics.builder()
                .productoId(producto.getId())
                .nombre(producto.getNombre())
                .categoria(producto.getCategoria().getDescripcion())
                .precioActual(producto.getPrecio())
                .ventasPeriodo(ventas != null ? ventas : 0L)
                .diasSinVenta(diasSinVenta)
                .participacionVentas(participacionVentas)
                .rankingGeneral(rankingGeneral)
                .rankingCategoria(rankingCat)
                .clientesDistintos(clientes != null ? clientes : 0L)
                .stockActual(stock != null ? stock : 0)
                .promedioPrecioCategoria(avgPriceCat)
                .promedioVentasCategoria(avgSalesCat)
                .stockPromedioCategoria(avgStockCat)
                .diferenciaPrecioCategoria(avgPriceCat != null ?
                        producto.getPrecio().subtract(avgPriceCat) : BigDecimal.ZERO)
                .build();
    }
}
