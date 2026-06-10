package com.recred.backend.application;

import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.ProductAdditionNotificationRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.services.PushNotificationSender;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;


public class AddProductRecommendation {

    private final ProductRepository productRepository;
    private final KiosqueroRepository kiosqueroRepository;
    private final InventoryRepository inventoryRepository;
    private final AIEventRepository aiEventRepository;
    private final ProductAdditionNotificationRepository notificationRepository;
    private final PushNotificationSender pushNotificationSender;


    public AddProductRecommendation(ProductRepository productRepository,
                                    KiosqueroRepository kiosqueroRepository,
                                    InventoryRepository inventoryRepository,
                                    AIEventRepository aiEventRepository,
                                    ProductAdditionNotificationRepository notificationRepository,
                                    PushNotificationSender pushNotificationSender) {
        this.productRepository = productRepository;
        this.kiosqueroRepository = kiosqueroRepository;
        this.inventoryRepository = inventoryRepository;
        this.aiEventRepository = aiEventRepository;
        this.notificationRepository = notificationRepository;
        this.pushNotificationSender = pushNotificationSender;
    }
    @Transactional
    public void execute() {

        List<Producto> bestSoldProducts = productRepository.findBestSoldProducts();
        List<Kiosquero> kiosqueros = kiosqueroRepository.findAll();

        for (Producto product : bestSoldProducts) {
            for (Kiosquero kiosquero : kiosqueros) {
                UUID buffetId = kiosqueroRepository.findBuffetById(kiosquero.getId());
                if(!this.alreadyInStock(product, buffetId)){

                    aiEventRepository
                            .registerProductAdditionRecommendation(product, kiosquero,buffetId);
                    
                    notificationRepository.save(product, kiosquero);
                    
                    PushNotification notification = new PushNotification(
                            kiosquero.getUsuario().getId(),
                            "Nuevo producto recomendado",
                            "Se recomienda agregar " + product.getNombre() + " a tu stock.",
                            Map.of("type", "SUGERENCIA_NUEVO_PRODUCTO")
                    );
                    pushNotificationSender.send(notification);
                }
            }
        }

    }

    private boolean alreadyInStock(Producto product,UUID buffetId) {
        return inventoryRepository.checkIfProductIsInStock(product.getId(),buffetId);
    }
}
