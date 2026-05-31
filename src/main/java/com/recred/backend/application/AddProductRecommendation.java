package com.recred.backend.application;

import com.recred.backend.domain.models.Merchant;
import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.MerchantRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


public class AddProductRecommendation {

    private final ProductRepository productRepository;
    private final MerchantRepository merchantRepository;
    private final InventoryRepository inventoryRepository;
    private final AIEventRepository aiEventRepository;


    public  AddProductRecommendation(ProductRepository productRepository,
                                     MerchantRepository merchantRepository,
            InventoryRepository inventoryRepository,
                                     AIEventRepository aiEventRepository){
        this.productRepository = productRepository;
        this.merchantRepository = merchantRepository;
        this.inventoryRepository = inventoryRepository;
        this.aiEventRepository = aiEventRepository;
    }
    @Transactional
    public void execute() {

        List<Product> bestSoldProducts = productRepository.findBestSoldProducts();
        List<Merchant> merchants = merchantRepository.findAll();

        for (Product product : bestSoldProducts) {
            for (Merchant merchant : merchants) {
                UUID buffetId = merchantRepository.findBuffetById(merchant.getId());
                if(!this.alreadyInStock(product, buffetId)){

                    aiEventRepository
                            .registerProductAdditionRecommendation(product, merchant,buffetId);
                }
            }
        }

    }

    private boolean alreadyInStock(Product product, UUID buffetId) {
        return inventoryRepository.checkIfProductIsInStock(product.getId(),buffetId);
    }
}
