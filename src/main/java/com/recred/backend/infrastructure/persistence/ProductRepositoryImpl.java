package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.ClasificacionTipo;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.ProductoMedida;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.entities.ClasificacionSaludEntity;
import com.recred.backend.infrastructure.entities.ClasificacionTipoEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.ProductoMedidaEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProductRepositoryImpl implements ProductRepository {

    private final SpringDataProductRepository springDataRepository;

    public ProductRepositoryImpl(SpringDataProductRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<Producto> findById(UUID id) {
        return springDataRepository.findById(id).map(ProductoEntity::toDomain);
    }

    @Override
    public Producto save(Producto producto) {
        ProductoEntity entityExistente;

        if (producto.getId() != null) {
            entityExistente = springDataRepository.findById(producto.getId())
                    .orElseGet(ProductoEntity::new);
        } else {
            entityExistente = new ProductoEntity();
        }

        ProductoEntity entityLista = toEntity(producto, entityExistente);
        ProductoEntity entityGuardada = springDataRepository.save(entityLista);
        return entityGuardada.toDomain();
    }

    private ProductoEntity toEntity(Producto modelo, ProductoEntity entity) {
        if (entity.getId() == null) {
            entity.setId(modelo.getId());
        }

        entity.setNombre(modelo.getNombre());
        entity.setDescripcion(modelo.getDescripcion());
        entity.setPrecio(modelo.getPrecio());
        entity.setPeso(modelo.getPeso());
        entity.setRequierePreparacion(modelo.getRequierePreparacion());
        entity.setActivo(modelo.getActivo());

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

    private boolean tieneMedias(Producto modelo) {
        if (modelo.getMedidas() == null) {
            return false;
        }
        return !modelo.getMedidas().isEmpty();
    }

    private boolean tieneTipos(Producto modelo) {
        if (modelo.getTipos() == null) {
            return false;
        }
        return !modelo.getTipos().isEmpty();
    }

    private boolean tieneClasificacionesSalud(Producto modelo) {
        if (modelo.getClasificacionesSalud() == null) {
            return false;
        }
        return !modelo.getClasificacionesSalud().isEmpty();
    }

    private void mapearCategoria(Producto modelo, ProductoEntity entity) {
        if (modelo.getCategoria() != null) {
            // Solo reasignamos si el ID de la categoría cambió para evitar toques innecesarios
            if (entity.getCategoria() == null
                    || !entity.getCategoria().getId().equals(modelo.getCategoria().getId())) {
                CategoriaProductoEntity categoriaEntity = new CategoriaProductoEntity();
                categoriaEntity.setId(modelo.getCategoria().getId());
                entity.setCategoria(categoriaEntity);
            }
        } else {
            // Si la categoría es nula, lanzar una excepción clara
            throw new IllegalArgumentException(
                    "La categoría del producto no puede ser nula");
        }
    }


    private void mapearMedidas(Producto modelo, ProductoEntity entity) {
        // 1. Limpiamos la colección actual (Hibernate registra las eliminaciones)
        entity.getMedidas().clear();
        // 2. Agregamos las nuevas usando tu método helper para mantener la bidireccionalidad
        modelo.getMedidas().forEach(medidaDominio -> {
            ProductoMedidaEntity medidaEntity = convertirMedida(medidaDominio);
            entity.addMedida(medidaEntity);
        });
    }

    private ProductoMedidaEntity convertirMedida(ProductoMedida medidaDominio) {
        ProductoMedidaEntity medidaEntity = new ProductoMedidaEntity();
        medidaEntity.setId(medidaDominio.getId());
        medidaEntity.setCantidadUnidad(medidaDominio.getCantidadUnidad());
        medidaEntity.setUnidad(medidaDominio.getUnidad());
        return medidaEntity;
    }

    private void limpiarMedidas(ProductoEntity entity) {
        if (entity.getMedidas() != null) {
            entity.getMedidas().clear();
        }
    }

    private void mapearTipos(Producto modelo, ProductoEntity entity) {
        // 1. Limpiamos la colección actual
        entity.getTipos().clear();
        // 2. Convertimos y agregamos todo a la colección existente
        Set<ClasificacionTipoEntity> tiposEntities = modelo.getTipos().stream()
                .map(this::convertirTipo)
                .collect(Collectors.toSet());
        entity.getTipos().addAll(tiposEntities);
    }

    private ClasificacionTipoEntity convertirTipo(ClasificacionTipo tipoDominio) {
        ClasificacionTipoEntity tipoEntity = new ClasificacionTipoEntity();
        tipoEntity.setId(tipoDominio.getId());
        tipoEntity.setDescripcion(tipoDominio.getDescripcion());
        return tipoEntity;
    }

    private void limpiarTipos(ProductoEntity entity) {
        if (entity.getTipos() != null) {
            entity.getTipos().clear();
        }
    }

    private void mapearClasificacionesSalud(Producto modelo, ProductoEntity entity) {
        // 1. Limpiamos la colección actual
        entity.getClasificacionesSalud().clear();
        // 2. Convertimos y agregamos todo a la colección existente
        Set<ClasificacionSaludEntity> clasificacionesEntities = modelo
                .getClasificacionesSalud().stream()
                .map(this::convertirClasificacionSalud)
                .collect(Collectors.toSet());
        entity.getClasificacionesSalud().addAll(clasificacionesEntities);
    }

    private ClasificacionSaludEntity convertirClasificacionSalud(
            ClasificacionSalud clasificacionDominio) {
        ClasificacionSaludEntity clasificacionEntity = new ClasificacionSaludEntity();
        clasificacionEntity.setId(clasificacionDominio.getId());
        clasificacionEntity.setDescripcion(clasificacionDominio.getDescripcion());
        clasificacionEntity.setActivo(clasificacionDominio.getActivo());
        return clasificacionEntity;
    }

    private void limpiarClasificacionesSalud(ProductoEntity entity) {
        if (entity.getClasificacionesSalud() != null) {
            entity.getClasificacionesSalud().clear();
        }
    }

}
