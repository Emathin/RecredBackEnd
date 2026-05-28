package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.Producto;

public interface ProductRecommendationRepository {
    void registerProductAdditionRecommendation(Producto product, Kiosquero kiosquero);
}
