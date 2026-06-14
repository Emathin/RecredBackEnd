package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.CartProductAffinity;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoRetiro;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PurchaseResume;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.CompraAnticipadaEntity;
import com.recred.backend.infrastructure.entities.CompraDetalleEntity;
import com.recred.backend.infrastructure.entities.CompraEntity;
import com.recred.backend.infrastructure.entities.CompraPresencialEntity;
import com.recred.backend.infrastructure.entities.FranjaHorariaEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.RetiroProgramadoEntity;
import com.recred.backend.infrastructure.entities.TutorEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    @Transactional(readOnly = true)
    public List<Purchase> findByAlumnoAndDateRange(
            UUID alumnoId, LocalDateTime start, LocalDateTime end) {
        return springDataRepository.findAllByAlumnoIdAndFechaHoraBetween(alumnoId, start, end)
                .stream()
                .peek(compra -> org.hibernate.Hibernate.initialize(compra.getAlumnoEntity()))
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

        if (purchase.getBuffetId() != null) {
            BuffetEntity buffetEntity = new BuffetEntity();
            buffetEntity.setId(purchase.getBuffetId());
            entity.setBuffetEntity(buffetEntity);
        }

        entity.setMedioPago(purchase.getPaymentMethod());
        entity.setEstado(purchase.getStatus());
        entity.setMontoTotal(purchase.getTotalAmount());
        entity.setFechaHora(purchase.getDate());
        entity.setSuggestionId(purchase.getSuggestionId());

        if (purchase.getTutorId() != null) {
            TutorEntity tutorEntity = new TutorEntity();
            tutorEntity.setId(purchase.getTutorId());
            entity.setTutorEntity(tutorEntity);
        }

        setAdvanceData(entity, purchase);

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
                .orElseGet(() -> newPurchaseEntity(purchase));
    }

    private CompraEntity newPurchaseEntity(Purchase purchase) {
        return purchase.getTipo() == TipoCompra.ANTICIPADA
                ? new CompraAnticipadaEntity()
                : new CompraPresencialEntity();
    }

    private void setAdvanceData(CompraEntity entity, Purchase purchase) {
        if (!(entity instanceof CompraAnticipadaEntity compraAnticipadaEntity)) {
            return;
        }

        compraAnticipadaEntity.setCodigoVerificacion(purchase.getWithdrawalCode());

        if (purchase.getPickupSlotId() == null || purchase.getPickupDate() == null) {
            return;
        }

        RetiroProgramadoEntity retiro = Optional.ofNullable(
                        compraAnticipadaEntity.getRetiroProgramadoEntity())
                .orElseGet(RetiroProgramadoEntity::new);
        FranjaHorariaEntity franja = new FranjaHorariaEntity();
        franja.setId(purchase.getPickupSlotId());
        retiro.setCompraEntity(compraAnticipadaEntity);
        retiro.setFranja(franja);
        retiro.setFechaProgramada(purchase.getPickupDate());
        if (purchase.getWithdrawalStatus() != null) {
            retiro.setEstado(purchase.getWithdrawalStatus());
        } else {
            retiro.setEstado(EstadoRetiro.PROGRAMADO);
        }
        compraAnticipadaEntity.setRetiroProgramadoEntity(retiro);
    }

    @Override
    public Optional<Purchase> findById(UUID id) {
        return springDataRepository.findById(id).map(CompraEntity::toDomain);
    }

    @Override
    public BigDecimal sumarGastosPorAlumnoEnRango(
            UUID alumnoId, LocalDateTime inicio, LocalDateTime fin) {
        BigDecimal presenciales = springDataRepository.sumarGastosPresencialesEnRango(
                alumnoId, EstadoCompra.ENTREGADO, inicio, fin);

        List<EstadoCompra> estadosAnticipados = List.of(
                EstadoCompra.ENTREGADO,
                EstadoCompra.PENDIENTE,
                EstadoCompra.EN_PREPARACION,
                EstadoCompra.LISTO
        );
        BigDecimal anticipadas = springDataRepository.sumarGastosAnticipadosEnRango(
                alumnoId, estadosAnticipados, inicio.toLocalDate(), fin.toLocalDate());

        return presenciales.add(anticipadas);
    }

    @Override
    public BigDecimal sumarGastosPorCategoriaEnRango(
            UUID alumnoId, UUID categoriaId, LocalDateTime inicio, LocalDateTime fin) {
        BigDecimal presenciales = springDataRepository.sumarGastosPresencialesPorCategoriaEnRango(
                alumnoId, EstadoCompra.ENTREGADO, categoriaId, inicio, fin);

        List<EstadoCompra> estadosAnticipados = List.of(
                EstadoCompra.ENTREGADO,
                EstadoCompra.PENDIENTE,
                EstadoCompra.EN_PREPARACION,
                EstadoCompra.LISTO
        );
        BigDecimal anticipadas = springDataRepository.sumarGastosAnticipadosPorCategoriaEnRango(
                alumnoId, estadosAnticipados, categoriaId, inicio.toLocalDate(), fin.toLocalDate());

        return presenciales.add(anticipadas);
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

    @Override
    public List<Purchase> findScheduledPickups(
            UUID buffetId,
            LocalDate fecha,
            EstadoRetiro estadoRetiro,
            UUID franjaId,
            String search) {
        return springDataRepository.findScheduledPickupsWithFilters(
                buffetId, fecha, estadoRetiro, franjaId, search)
                .stream()
                .map(CompraEntity::toDomain)
                .toList();
    }

    @Override
    public long countHealthyPurchasesByAlumno(UUID alumnoId, LocalDateTime since) {
        return springDataRepository
                .countByAlumnoEntityIdAndSuggestionIdIsNotNullAndEstadoAndFechaHoraAfter(
                        alumnoId, EstadoCompra.ENTREGADO, since);
    }

}
