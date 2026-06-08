package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.CartProductAffinity;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PurchaseResume;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.CompraDetalleEntity;
import com.recred.backend.infrastructure.entities.CompraEntity;
import com.recred.backend.infrastructure.entities.CompraPresencialEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PurchaseRepositoryImpl implements PurchaseRepository {

    private final SpringDataPurchaseRepository springDataRepository;

    public PurchaseRepositoryImpl(SpringDataPurchaseRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public List<Purchase> findHistorialByAlumno(UUID alumnoId) {
        return springDataRepository.findAllByAlumnoIdOrderByFechaHoraDesc(alumnoId)
                .stream()
                .map(CompraEntity::toDomain)
                .toList();
    }

    @Override
    public List<Purchase> findByAlumnoAndDateRange(
            UUID alumnoId, LocalDateTime start, LocalDateTime end) {
        return springDataRepository.findAllByAlumnoIdAndFechaHoraBetween(alumnoId, start, end)
                .stream()
                .map(CompraEntity::toDomain)
                .toList();
    }

    @Override
    public List<Purchase> findHistorialByTutor(UUID tutorId) {
        return springDataRepository.findAllByTutorIdOrderByFechaHoraDesc(tutorId)
                .stream()
                .map(CompraEntity::toDomain)
                .toList();
    }

    @Override
    public Purchase save(Purchase purchase) {
        CompraEntity entity = findEntityToSave(purchase);

        if (purchase.getId() != null) {
            entity.setId(purchase.getId());
        }

        AlumnoEntity alumnoEntity = new AlumnoEntity();
        alumnoEntity.setId(purchase.getStudent().getId());
        entity.setAlumnoEntity(alumnoEntity);

        entity.setMedioPago(purchase.getPaymentMethod());
        entity.setEstado(purchase.getStatus());
        entity.setMontoTotal(purchase.getTotalAmount());
        entity.setFechaHora(purchase.getDate());

        entity.getDetalles().clear();
        purchase.getItems().forEach((producto, cantidad) -> {
            CompraDetalleEntity detalle = new CompraDetalleEntity();

            ProductoEntity productoEntity = new ProductoEntity();
            productoEntity.setId(producto.getId());
            detalle.setProductoEntity(productoEntity);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(producto.getPrecio().multiply(new BigDecimal(cantidad)));

            entity.addDetalle(detalle);
        });

        CompraEntity savedEntity = springDataRepository.save(entity);
        purchase.setId(savedEntity.getId());

        return savedEntity.toDomain();
    }

    private CompraEntity findEntityToSave(Purchase purchase) {
        return Optional.ofNullable(purchase.getId())
                .flatMap(springDataRepository::findById)
                .orElseGet(CompraPresencialEntity::new);
    }

    @Override
    public Optional<Purchase> findById(UUID id) {
        return springDataRepository.findById(id).map(CompraEntity::toDomain);
    }

    @Override
    public BigDecimal sumarGastosPorAlumnoEnRango
            (UUID alumnoId, LocalDateTime inicio, LocalDateTime fin) {
        return springDataRepository.sumarGastosGeneralesEnRango
                (alumnoId, EstadoCompra.ENTREGADO, inicio, fin);

    }

    @Override
    public BigDecimal sumarGastosPorCategoriaEnRango
            (UUID alumnoId, UUID categoriaId, LocalDateTime inicio, LocalDateTime fin) {
        return springDataRepository.sumarGastosPorCategoriaEnRango
                (alumnoId, EstadoCompra.ENTREGADO, categoriaId, inicio, fin);
    }

    @Override
    public List<PreferenceAIRepository.CompraResumen> findLastPurchasesByAlumno(
            UUID alumnoId, int limit) {
        List<CompraEntity> compras =
                springDataRepository.findLastByAlumno(alumnoId, PageRequest.of(0, limit));

        return compras.stream()
                .flatMap(c -> c.getDetalles().stream())
                .map(d -> {
                    String categoryName = "Sin Categoria";
                    if (d.getProductoEntity().getCategoria() != null) {
                        categoryName = d.getProductoEntity().getCategoria().getDescripcion();
                    }
                    return new PreferenceAIRepository.CompraResumen(
                            d.getProductoEntity().getNombre(),
                            categoryName,
                            d.getCompra().getFechaHora()
                    );
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<PurchaseResume> findPurchasesByAlumno(UUID alumnoId) {
        return springDataRepository.findByAlumno(alumnoId);
    }

    @Override
    public List<Purchase> findRecentByBuffet(UUID buffetId, int limit) {
        return springDataRepository.findRecentByBuffetId(buffetId, PageRequest.of(0, limit))
                .stream()
                .map(CompraEntity::toDomain)
                .toList();
    }

    @Override
    public List<Purchase> findByBuffetAndStatuses(UUID buffetId,
                                                  List<EstadoCompra> statuses,
                                                  int limit) {
        return springDataRepository.findByBuffetIdAndStatuses(
                        buffetId,
                        statuses,
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(CompraEntity::toDomain)
                .toList();
    }

    @Override
    public List<CartProductAffinity> findStudentCartAffinities(
            UUID alumnoId,
            UUID buffetId,
            Set<UUID> cartProductIds) {
        if (cartProductIds == null || cartProductIds.isEmpty()) {
            return List.of();
        }
        return springDataRepository.findStudentCartAffinities(
                alumnoId,
                buffetId,
                EstadoCompra.ENTREGADO,
                List.copyOf(cartProductIds));
    }

    @Override
    public List<CartProductAffinity> findBuffetCartAffinities(
            UUID buffetId,
            Set<UUID> cartProductIds) {
        if (cartProductIds == null || cartProductIds.isEmpty()) {
            return List.of();
        }
        return springDataRepository.findBuffetCartAffinities(
                buffetId,
                EstadoCompra.ENTREGADO,
                List.copyOf(cartProductIds));
    }

}
