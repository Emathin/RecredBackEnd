package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.PurchaseState;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.infrastructure.entities.StudentEntity;
import com.recred.backend.infrastructure.entities.ProductEntity;
import com.recred.backend.infrastructure.entities.PurchaseDetailEntity;
import com.recred.backend.infrastructure.entities.PurchaseEntity;
import com.recred.backend.infrastructure.entities.PresentialPurchaseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PurchaseRepositoryImpl implements PurchaseRepository {

    private final SpringDataPurchaseRepository springDataRepository;

    public PurchaseRepositoryImpl(SpringDataPurchaseRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Purchase save(Purchase purchase) {
        PurchaseEntity entity = findEntityToSave(purchase);

        if (purchase.getId() != null) {
            entity.setId(purchase.getId());
        }

        StudentEntity studentEntity = new StudentEntity();
        studentEntity.setId(purchase.getStudent().getId());
        entity.setStudentEntity(studentEntity);

        entity.setPaymentMethod(purchase.getPaymentMethod());
        entity.setPurchaseState(purchase.getStatus());
        entity.setTotalAmount(purchase.getTotalAmount());
        entity.setDateAndTime(purchase.getDate());

        entity.getDetails().clear();
        purchase.getItems().forEach((producto, cantidad) -> {
            PurchaseDetailEntity detalle = new PurchaseDetailEntity();

            ProductEntity productEntity = new ProductEntity();
            productEntity.setId(producto.getId());
            detalle.setProductEntity(productEntity);
            detalle.setAmount(cantidad);
            detalle.setUnitPrice(producto.getPrice());
            detalle.setSubtotal(producto.getPrice().multiply(new BigDecimal(cantidad)));

            entity.addDetalle(detalle);
        });

        PurchaseEntity savedEntity = springDataRepository.save(entity);
        purchase.setId(savedEntity.getId());

        return savedEntity.toDomain();
    }

    private PurchaseEntity findEntityToSave(Purchase purchase) {
        return Optional.ofNullable(purchase.getId())
                .flatMap(springDataRepository::findById)
                .orElseGet(PresentialPurchaseEntity::new);
    }

    @Override
    public Optional<Purchase> findById(UUID id) {
        return springDataRepository.findById(id).map(PurchaseEntity::toDomain);
    }

    @Override
    public BigDecimal sumExpensesByStudentInRange
            (UUID alumnoId, LocalDateTime inicio, LocalDateTime fin) {
        return springDataRepository.sumarGastosGeneralesEnRango
                (alumnoId, PurchaseState.ENTREGADO, inicio, fin);

    }

    @Override
    public BigDecimal sumExpensesByCategoryInRange
            (UUID alumnoId, UUID categoriaId, LocalDateTime inicio, LocalDateTime fin) {
        return springDataRepository.sumarGastosPorCategoriaEnRango
                (alumnoId, PurchaseState.ENTREGADO, categoriaId, inicio, fin);
    }

    @Override
    public List<PreferenceAIRepository.PurchaseSummary> findLastPurchasesByStudent(
            UUID alumnoId, int limit) {
        List<PurchaseEntity> compras =
                springDataRepository.findLastByAlumno(alumnoId, PageRequest.of(0, limit));

        return compras.stream()
                .flatMap(c -> c.getDetails().stream())
                .map(d -> {
                    String categoryName = "Sin Categoria";
                    if (d.getProductEntity().getCategory() != null) {
                        categoryName = d.getProductEntity().getCategory().getDescription();
                    }
                    return new PreferenceAIRepository.PurchaseSummary(
                            d.getProductEntity().getName(),
                            categoryName,
                            d.getPurchase().getDateAndTime()
                    );
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

}
