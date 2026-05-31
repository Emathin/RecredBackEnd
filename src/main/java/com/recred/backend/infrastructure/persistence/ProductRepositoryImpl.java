package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.models.TypeClassification;
import com.recred.backend.domain.models.HealthClassification;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.ProductMeasure;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.infrastructure.entities.ProductCategoryEntity;
import com.recred.backend.infrastructure.entities.HealthClassificationEntity;
import com.recred.backend.infrastructure.entities.ProductEntity;
import com.recred.backend.infrastructure.entities.TypeClassificationEntity;
import com.recred.backend.infrastructure.entities.ProductMeasureEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProductRepositoryImpl implements ProductRepository {

    private final SpringDataProductRepository springDataRepository;

    public ProductRepositoryImpl(
            SpringDataProductRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return springDataRepository.findById(id).map(ProductEntity::toDomain);
    }
    @Override
    public ProductChangeRecommendation findBuffetLeastSoldProducts(UUID buffetId) {
        ProductEntity producto = springDataRepository.findBuffetLeastSoldProducts(buffetId);

        return producto.toProductRecommendation(buffetId);
    }

    @Override
    public List<Product> findBestSoldProducts() {
        List<ProductEntity> productos = springDataRepository.findBestSoldProducts();
        return productos.stream().map(ProductEntity::toDomain).toList();
    }

    @Override
    public Product save(Product product) {
        ProductEntity entityExistente;

        if (product.getId() != null) {
            entityExistente = springDataRepository.findById(product.getId())
                    .orElseGet(ProductEntity::new);
        } else {
            entityExistente = new ProductEntity();
        }

        ProductEntity entityLista = toEntity(product, entityExistente);
        ProductEntity entityGuardada = springDataRepository.save(entityLista);
        return entityGuardada.toDomain();
    }

    @Override
    public void updatePrice(UUID productId, BigDecimal newPrice) {
        springDataRepository.updatePrice(productId, newPrice);
    }

    private ProductEntity toEntity(Product modelo, ProductEntity entity) {
        if (entity.getId() == null) {
            entity.setId(modelo.getId());
        }

        entity.setName(modelo.getName());
        entity.setDescription(modelo.getDescription());
        entity.setPrice(modelo.getPrice());
        entity.setWeight(modelo.getWeight());
        entity.setRequiresPreparation(modelo.getRequiresPreparation());
        entity.setActive(modelo.getActive());

        mapearCategoria(modelo, entity);

        if (tieneMedias(modelo)) {
            mapearMedidas(modelo, entity);
        } else {
            limpiarMedidas(entity);
        }

        if (tieneTipos(modelo)) {
            mapearTipos(modelo, entity);
        } else {
            limpiarTipos(entity);
        }

        if (tieneClasificacionesSalud(modelo)) {
            mapearClasificacionesSalud(modelo, entity);
        } else {
            limpiarClasificacionesSalud(entity);
        }

        return entity;
    }

    private boolean tieneMedias(Product modelo) {
        if (modelo.getMeasures() == null) {
            return false;
        }
        return !modelo.getMeasures().isEmpty();
    }

    private boolean tieneTipos(Product modelo) {
        if (modelo.getTypes() == null) {
            return false;
        }
        return !modelo.getTypes().isEmpty();
    }

    private boolean tieneClasificacionesSalud(Product modelo) {
        if (modelo.getHealthClassifications() == null) {
            return false;
        }
        return !modelo.getHealthClassifications().isEmpty();
    }

    private void mapearCategoria(Product modelo, ProductEntity entity) {
        if (modelo.getCategory() != null) {
            if (entity.getCategory() == null
                    || !entity.getCategory().getId().equals(modelo.getCategory().getId())) {
                ProductCategoryEntity categoriaEntity = new ProductCategoryEntity();
                categoriaEntity.setId(modelo.getCategory().getId());
                entity.setCategory(categoriaEntity);
            }
        } else {

            throw new IllegalArgumentException(
                    "La categoría del producto no puede ser nula");
        }
    }


    private void mapearMedidas(Product modelo, ProductEntity entity) {

        entity.getMeasures().clear();

        modelo.getMeasures().forEach(medidaDominio -> {
            ProductMeasureEntity medidaEntity = convertirMedida(medidaDominio);
            entity.addMedida(medidaEntity);
        });
    }

    private ProductMeasureEntity convertirMedida(ProductMeasure medidaDominio) {
        ProductMeasureEntity medidaEntity = new ProductMeasureEntity();
        medidaEntity.setId(medidaDominio.getId());
        medidaEntity.setUnitQuantity(medidaDominio.getUnitAmount());
        medidaEntity.setUnit(medidaDominio.getUnit());
        return medidaEntity;
    }

    private void limpiarMedidas(ProductEntity entity) {
        if (entity.getMeasures() != null) {
            entity.getMeasures().clear();
        }
    }

    private void mapearTipos(Product modelo, ProductEntity entity) {

        entity.getTypes().clear();

        Set<TypeClassificationEntity> tiposEntities = modelo.getTypes().stream()
                .map(this::convertirTipo)
                .collect(Collectors.toSet());
        entity.getTypes().addAll(tiposEntities);
    }

    private TypeClassificationEntity convertirTipo(TypeClassification tipoDominio) {
        TypeClassificationEntity tipoEntity = new TypeClassificationEntity();
        tipoEntity.setId(tipoDominio.getId());
        tipoEntity.setDescription(tipoDominio.getDescription());
        return tipoEntity;
    }

    private void limpiarTipos(ProductEntity entity) {
        if (entity.getTypes() != null) {
            entity.getTypes().clear();
        }
    }

    private void mapearClasificacionesSalud(Product modelo, ProductEntity entity) {

        entity.getHealthClassifications().clear();

        Set<HealthClassificationEntity> clasificacionesEntities = modelo
                .getHealthClassifications().stream()
                .map(this::convertirClasificacionSalud)
                .collect(Collectors.toSet());
        entity.getHealthClassifications().addAll(clasificacionesEntities);
    }

    private HealthClassificationEntity convertirClasificacionSalud(
            HealthClassification clasificacionDominio) {
        HealthClassificationEntity clasificacionEntity = new HealthClassificationEntity();
        clasificacionEntity.setId(clasificacionDominio.getId());
        clasificacionEntity.setDescription(clasificacionDominio.getDescription());
        clasificacionEntity.setActive(clasificacionDominio.getActive());
        return clasificacionEntity;
    }

    private void limpiarClasificacionesSalud(ProductEntity entity) {
        if (entity.getHealthClassifications() != null) {
            entity.getHealthClassifications().clear();
        }
    }

}
