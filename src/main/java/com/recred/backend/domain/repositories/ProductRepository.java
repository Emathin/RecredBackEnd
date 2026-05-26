package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.infrastructure.entities.ProductoEntity;

import java.util.List;
import java.util.UUID;

public interface ProductRepository {
    ProductChangeRecommendation findBuffetLeastSoldProducts(UUID buffetId);
}
