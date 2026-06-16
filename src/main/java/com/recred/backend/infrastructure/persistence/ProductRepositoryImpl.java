package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.ClasificacionTipo;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.ProductoMedida;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.entities.ClasificacionSaludEntity;
import com.recred.backend.infrastructure.entities.ClasificacionTipoEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.ProductoMedidaEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(readOnly = true)
    public Optional<Producto> findById(UUID id) {
        return springDataRepository.findById(id).map(ProductoEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> findAllActive() {
        return springDataRepository.findAllByActivoTrue()
                .stream()
                .map(ProductoEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Producto> findActiveById(UUID id) {
        return springDataRepository.findByIdAndActivoTrue(id)
                .map(ProductoEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> findAllActiveWithStock(UUID buffetId) {
        return springDataRepository.findAllActiveWithStockParams(buffetId)
                .stream()
                .map(this::mapObjectArrayToDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Producto> findActiveByIdWithStock(UUID id, UUID buffetId) {
        return springDataRepository.findActiveByIdWithStockParams(id, buffetId)
                .map(this::mapObjectArrayToDomain);
    }

    private Producto mapObjectArrayToDomain(Object[] result) {
        ProductoEntity entity = (ProductoEntity) result[0];
        Integer stockActual = integerValue(result[1]);
        Integer stockReservado = integerValue(result[2]);
        Integer stockMinimo = integerValue(result[3]);
        EstadoInventario estado = (EstadoInventario) result[4];
        TipoManejoInventario tipoManejo = (TipoManejoInventario) result[5];
        Integer cupoMaximoDiario = integerValue(result[6]);
        Integer stockDisponible = integerValue(result[7]);
        String urlImagen = result.length > 8 ? (String) result[8] : null;
        Inventario inventario = inventario(
                stockActual,
                stockReservado,
                stockMinimo,
                estado,
                tipoManejo
        );
        Producto domain = entity.toDomain();
        domain.setStockActual(stockActual);
        domain.setStockReservado(stockReservado);
        domain.setStockMinimo(inventario == null
                ? stockMinimo
                : inventario.stockMinimoEfectivo());
        domain.setEstadoInventario(inventario == null ? estado : inventario.estadoActual());
        domain.setTipoManejoInventario(tipoManejo);
        domain.setCupoMaximoDiario(cupoMaximoDiario);
        domain.setStockDisponible(stockDisponible);
        domain.setUrlImagen(urlImagen);
        return domain;
    }

    private Inventario inventario(
            Integer stockActual,
            Integer stockReservado,
            Integer stockMinimo,
            EstadoInventario estado,
            TipoManejoInventario tipoManejo
    ) {
        return tieneDatosDeInventario(stockActual, stockMinimo, estado, tipoManejo)
                ? crearInventario(stockActual, stockReservado, stockMinimo, estado, tipoManejo)
                : null;
    }

    private Inventario crearInventario(
            Integer stockActual,
            Integer stockReservado,
            Integer stockMinimo,
            EstadoInventario estado,
            TipoManejoInventario tipoManejo
    ) {
        Inventario inventario = new Inventario();
        inventario.setStockActual(stockActual);
        inventario.setStockReservado(stockReservado);
        inventario.setStockMinimo(stockMinimo);
        inventario.setEstado(estado);
        inventario.setTipoManejo(tipoManejo);
        return inventario;
    }

    private boolean tieneDatosDeInventario(
            Integer stockActual,
            Integer stockMinimo,
            EstadoInventario estado,
            TipoManejoInventario tipoManejo
    ) {
        return stockActual != null
                || stockMinimo != null
                || estado != null
                || tipoManejo != null;
    }

    private Integer integerValue(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductChangeRecommendation findBuffetLeastSoldProducts(UUID buffetId) {
        ProductoEntity producto = springDataRepository.findBuffetLeastSoldProducts(buffetId);

        return producto.toProductRecommendation(buffetId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductChangeRecommendation> findBuffetTop5LeastSoldProducts(UUID buffetId) {
        List<ProductoEntity> productos =
                springDataRepository.findBuffetTop5LeastSoldProducts(buffetId);

        return productos.stream()
                .map(p -> p.toProductRecommendation(buffetId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> findBestSoldProducts() {
        List<ProductoEntity> productos = springDataRepository.findBestSoldProducts();
        return productos.stream().map(ProductoEntity::toDomain).toList();
    }

    @Override
    @Transactional
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

    @Override
    @Transactional
    public void updatePrice(UUID productId, BigDecimal newPrice) {
        springDataRepository.updatePrice(productId, newPrice);
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
            if (entity.getCategoria() == null
                    || !entity.getCategoria().getId().equals(modelo.getCategoria().getId())) {
                CategoriaProductoEntity categoriaEntity = new CategoriaProductoEntity();
                categoriaEntity.setId(modelo.getCategoria().getId());
                entity.setCategoria(categoriaEntity);
            }
        } else {

            throw new IllegalArgumentException(
                    "La categoría del producto no puede ser nula");
        }
    }


    private void mapearMedidas(Producto modelo, ProductoEntity entity) {

        entity.getMedidas().clear();

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

        entity.getTipos().clear();

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

        entity.getClasificacionesSalud().clear();

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
