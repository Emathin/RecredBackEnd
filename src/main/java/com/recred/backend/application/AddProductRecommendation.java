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
import org.jspecify.annotations.NonNull;
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
    public AddProductRecommendationResponse execute(AddProductRecommendationRequest request) {
        List<Kiosquero> kiosqueros = kiosqueroRepository.findAll();
        LocalDateTime since = LocalDateTime.now().minusMonths(1);

        for (Kiosquero kiosquero : kiosqueros) {
                List<Producto> missingProducts = getBuffetMissingProducts(kiosquero);
            for (Producto product : missingProducts) {
                recommendProduct(kiosquero, product, since);
            }
        }
        return new AddProductRecommendationResponse();
    }

    private UUID getBuffetIdForKiosquero(Kiosquero kiosquero) {
        return kiosqueroRepository.findBuffetById(kiosquero.getId());
    }

    private List<Producto> getBuffetMissingProducts(Kiosquero kiosquero) {
        return productRepository
                .findBestSoldProductsNotInStock(getBuffetIdForKiosquero(kiosquero));
    }

    private void recommendProduct(Kiosquero kiosquero, Producto product, LocalDateTime since) {
        aiEventRepository.registerProductAdditionRecommendation(
                product, kiosquero, getBuffetIdForKiosquero(kiosquero));
        sugerenciaIARepository.save(buildProductSuggestion(
                product, getBuffetIdForKiosquero(kiosquero), getProductStatistics(product, since)));
        notificationRepository.save(product, kiosquero, buildProductSuggestion(
                product, getBuffetIdForKiosquero(kiosquero), getProductStatistics(product, since)));
    }

    private @NonNull Map<String, Object> getProductStatistics(
            Producto product, LocalDateTime since) {
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
