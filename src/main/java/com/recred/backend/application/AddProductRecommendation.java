package com.recred.backend.application;

import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.ProductAdditionNotificationRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.ProductStatisticsRepository;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class AddProductRecommendation {

    private final ProductRepository productRepository;
    private final KiosqueroRepository kiosqueroRepository;
    private final AIEventRepository aiEventRepository;
    private final ProductAdditionNotificationRepository notificationRepository;
    private final ProductStatisticsRepository productStatisticsRepository;
    private final SugerenciaIARepository sugerenciaIARepository;


    public AddProductRecommendation(
            ProductRepository productRepository,
            KiosqueroRepository kiosqueroRepository,
            AIEventRepository aiEventRepository,
            ProductAdditionNotificationRepository notificationRepository,
            ProductStatisticsRepository productStatisticsRepository,
            SugerenciaIARepository sugerenciaIARepository) {
        this.productRepository = productRepository;
        this.kiosqueroRepository = kiosqueroRepository;
        this.aiEventRepository = aiEventRepository;
        this.notificationRepository = notificationRepository;
        this.productStatisticsRepository = productStatisticsRepository;
        this.sugerenciaIARepository = sugerenciaIARepository;
    }

    @Transactional
    public AddProductRecommendationResponse execute(AddProductRecommendationRequest addRequest) {
        recommendMissingProductsForKiosqueros(
                findKiosqueros(),
                recommendationPeriodStart());

        return new AddProductRecommendationResponse();
    }

    private List<Kiosquero> findKiosqueros() {
        return kiosqueroRepository.findAll();
    }

    private LocalDateTime recommendationPeriodStart() {
        return LocalDateTime.now().minusMonths(1);
    }

    private void recommendMissingProductsForKiosqueros(
            List<Kiosquero> kiosqueros,
            LocalDateTime since) {
        for (Kiosquero kiosquero : kiosqueros) {
            recommendMissingProducts(kiosquero, since);
        }
    }

    private void recommendMissingProducts(Kiosquero kiosquero, LocalDateTime since) {
        UUID buffetId = findBuffetIdForKiosquero(kiosquero);
        List<Producto> missingProducts = findBuffetMissingProducts(buffetId);

        for (Producto product : missingProducts) {
            recommendProduct(kiosquero, product, buffetId, since);
        }
    }

    private UUID findBuffetIdForKiosquero(Kiosquero kiosquero) {
        return kiosqueroRepository.findBuffetById(kiosquero.getId());
    }

    private List<Producto> findBuffetMissingProducts(UUID buffetId) {
        return productRepository.findBestSoldProductsNotInStock(buffetId);
    }

    private void recommendProduct(Kiosquero kiosquero, Producto product, UUID buffetId,
                                  LocalDateTime since) {
        SugerenciaIA suggestion = buildProductSuggestion(
                product,
                buffetId,
                getProductStatistics(product, since));

        aiEventRepository.registerProductAdditionRecommendation(
                product,
                kiosquero,
                buffetId);
        sugerenciaIARepository.save(suggestion);
        notificationRepository.save(product, kiosquero, suggestion);
    }

    private Map<String, Object> getProductStatistics(Producto product, LocalDateTime since) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("productName", product.getNombre());
        stats.put("productPrice", product.getPrecio());
        stats.put("totalSales",
                productStatisticsRepository
                        .countTotalSalesByProduct(product.getId(), since));
        stats.put("totalCustomers",
                productStatisticsRepository
                        .countDistinctCustomersByProduct(product.getId(), since));
        stats.put("totalRevenue",
                productStatisticsRepository
                        .calculateTotalRevenueByProduct(product.getId(), since));
        return stats;
    }

    private static SugerenciaIA buildProductSuggestion(
            Producto product, UUID buffetId, Map<String, Object> stats) {
        return SugerenciaIA.builder()
                .tipo(TipoSugerenciaIA.AGREGAR_PRODUCTO)
                .buffetId(buffetId)
                .productoId(product.getId())
                .titulo("Sugerencia de producto")
                .mensaje("Te recomendamos agregar "
                        + product.getNombre() + " a tu stock")
                .metadata(stats)
                .build();
    }

}
