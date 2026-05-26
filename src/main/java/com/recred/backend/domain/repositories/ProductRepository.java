package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.ProductChangeRecommendation;
import java.util.UUID;

public interface ProductRepository {
    ProductChangeRecommendation findBuffetLeastSoldProducts(UUID buffetId);
}
