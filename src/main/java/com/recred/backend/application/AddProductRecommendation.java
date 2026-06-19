package com.recred.backend.application;

import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.InventoryRepository;
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
    private final InventoryRepository inventoryRepository;
    private final AIEventRepository aiEventRepository;
    private final ProductAdditionNotificationRepository notificationRepository;
    private final ProductStatisticsRepository productStatisticsRepository;
    private final SugerenciaIARepository sugerenciaIARepository;


    public AddProductRecommendation(ProductRepository productRepository,
                                    KiosqueroRepository kiosqueroRepository,
                                    InventoryRepository inventoryRepository,
                                    AIEventRepository aiEventRepository,
                                    ProductAdditionNotificationRepository notificationRepository,
                                    ProductStatisticsRepository productStatisticsRepository,
                                    SugerenciaIARepository sugerenciaIARepository) {
        this.productRepository = productRepository;
        this.kiosqueroRepository = kiosqueroRepository;
        this.inventoryRepository = inventoryRepository;
        this.aiEventRepository = aiEventRepository;
        this.notificationRepository = notificationRepository;
        this.productStatisticsRepository = productStatisticsRepository;
        this.sugerenciaIARepository = sugerenciaIARepository;
    }
    @Transactional
    public void execute() {

        List<Producto> bestSoldProducts = productRepository.findBestSoldProducts();
        List<Kiosquero> kiosqueroUsers = kiosqueroRepository.findAll();
        LocalDateTime since = LocalDateTime.now().minusMonths(1);

        for (Producto product : bestSoldProducts) {

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
            
            for (Kiosquero kiosquero : kiosqueroUsers) {
                UUID buffetId = kiosqueroRepository.findBuffetById(kiosquero.getId());
                if (buffetId != null && !this.alreadyInStock(product, buffetId)) {

                    aiEventRepository
                            .registerProductAdditionRecommendation(product, kiosquero, buffetId);

                    SugerenciaIA suggestion = SugerenciaIA.builder()
                            .tipo(TipoSugerenciaIA.AGREGAR_PRODUCTO)
                            .buffetId(buffetId)
                            .productoId(product.getId())
                            .titulo("Sugerencia de producto")
                            .mensaje("Te recomendamos agregar "
                                    + product.getNombre() + " a tu stock")
                            .metadata(stats)
                            .build();

                    sugerenciaIARepository.save(suggestion);

                    notificationRepository.save(product, kiosquero, suggestion);
                }
            }
        }

    }

    private boolean alreadyInStock(Producto product, UUID buffetId) {
        return inventoryRepository.checkIfProductIsInStock(product.getId(), buffetId);
    }
}
