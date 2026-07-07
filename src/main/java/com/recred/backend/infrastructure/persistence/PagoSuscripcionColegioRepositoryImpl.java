package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.PagoSuscripcionColegio;
import com.recred.backend.domain.repositories.PagoSuscripcionColegioRepository;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.PagoSuscripcionColegioEntity;
import com.recred.backend.infrastructure.entities.SuscripcionColegioEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class PagoSuscripcionColegioRepositoryImpl
        implements PagoSuscripcionColegioRepository {

    private final SpringDataPagoSuscripcionColegioRepository springDataRepository;

    public PagoSuscripcionColegioRepositoryImpl(
            SpringDataPagoSuscripcionColegioRepository springDataRepository
    ) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PagoSuscripcionColegio> findByMercadoPagoPaymentId(String paymentId) {
        return springDataRepository.findByMercadoPagoPaymentId(paymentId)
                .map(PagoSuscripcionColegioEntity::toDomain);
    }

    @Override
    @Transactional
    public PagoSuscripcionColegio save(PagoSuscripcionColegio payment) {
        PagoSuscripcionColegioEntity entity = existingEntity(payment);
        PagoSuscripcionColegioEntity saved = springDataRepository.save(toEntity(payment, entity));
        return saved.toDomain();
    }

    private PagoSuscripcionColegioEntity existingEntity(PagoSuscripcionColegio payment) {
        if (payment.getId() == null) {
            return new PagoSuscripcionColegioEntity();
        }

        return springDataRepository.findById(payment.getId())
                .orElse(new PagoSuscripcionColegioEntity());
    }

    private PagoSuscripcionColegioEntity toEntity(
            PagoSuscripcionColegio payment,
            PagoSuscripcionColegioEntity entity
    ) {
        if (entity.getId() == null) {
            entity.setId(payment.getId());
        }

        entity.setMercadoPagoPaymentId(payment.getMercadoPagoPaymentId());
        entity.setColegioEntity(schoolReference(payment.getColegioId()));
        entity.setSuscripcionEntity(subscriptionReference(payment.getSuscripcionId()));
        entity.setMonto(payment.getMonto());
        entity.setMoneda(payment.getMoneda());
        entity.setEstado(payment.getEstado());
        entity.setCreadoEn(payment.getCreadoEn());
        entity.setAprobadoEn(payment.getAprobadoEn());
        return entity;
    }

    private ColegioEntity schoolReference(UUID colegioId) {
        ColegioEntity entity = new ColegioEntity();
        entity.setId(colegioId);
        return entity;
    }

    private SuscripcionColegioEntity subscriptionReference(UUID subscriptionId) {
        if (subscriptionId == null) {
            return null;
        }

        SuscripcionColegioEntity entity = new SuscripcionColegioEntity();
        entity.setId(subscriptionId);
        return entity;
    }
}
