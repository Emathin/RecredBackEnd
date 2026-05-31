package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Merchant;
import com.recred.backend.domain.models.Product;

public interface ProductRecommendationRepository {
    void registerProductAdditionRecommendation(Product product, Merchant merchant);
}
