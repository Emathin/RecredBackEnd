package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProductRepositoryImpl implements ProductRepository {

    private final SpringDataProductRepository springDataRepository;

    public ProductRepositoryImpl(
                                 SpringDataProductRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<Producto> findById(UUID id) {
        return springDataRepository.findById(id).map(ProductoEntity::toDomain);
    }
    @Override
    public ProductChangeRecommendation findBuffetLeastSoldProducts(UUID buffetId) {
        ProductoEntity producto = springDataRepository.findBuffetLeastSoldProducts(buffetId);

        return producto.toProductRecommendation(buffetId);
    }

    @Override
    public List<Producto> findBestSoldProducts() {
        List<ProductoEntity> productos = springDataRepository.findBestSoldProducts();
        return productos.stream().map(ProductoEntity::toDomain).toList();
    }

}
