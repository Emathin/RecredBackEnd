package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.PagoSuscripcionUsuario;
import com.recred.backend.domain.repositories.PagoSuscripcionUsuarioRepository;
import com.recred.backend.infrastructure.entities.PagoSuscripcionUsuarioEntity;
import com.recred.backend.infrastructure.entities.SuscripcionUsuarioEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class PagoSuscripcionUsuarioRepositoryImpl
        implements PagoSuscripcionUsuarioRepository {

    private final SpringDataPagoSuscripcionUsuarioRepository springDataRepository;

    public PagoSuscripcionUsuarioRepositoryImpl(
            SpringDataPagoSuscripcionUsuarioRepository springDataRepository
    ) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PagoSuscripcionUsuario> findByMercadoPagoPaymentId(String paymentId) {
        return springDataRepository.findByMercadoPagoPaymentId(paymentId)
                .map(PagoSuscripcionUsuarioEntity::toDomain);
    }

    @Override
    @Transactional
    public PagoSuscripcionUsuario save(PagoSuscripcionUsuario payment) {
        PagoSuscripcionUsuarioEntity entity = existingEntity(payment);
        PagoSuscripcionUsuarioEntity saved = springDataRepository.saveAndFlush(
                toEntity(payment, entity));
        return saved.toDomain();
    }

    private PagoSuscripcionUsuarioEntity existingEntity(PagoSuscripcionUsuario payment) {
        if (payment.getId() == null) {
            return new PagoSuscripcionUsuarioEntity();
        }

        return springDataRepository.findById(payment.getId())
                .orElse(new PagoSuscripcionUsuarioEntity());
    }

    private PagoSuscripcionUsuarioEntity toEntity(
            PagoSuscripcionUsuario payment,
            PagoSuscripcionUsuarioEntity entity
    ) {
        if (entity.getId() == null) {
            entity.setId(payment.getId());
        }

        entity.setMercadoPagoPaymentId(payment.getMercadoPagoPaymentId());
        entity.setUsuarioEntity(userReference(payment.getUsuarioId()));
        entity.setSuscripcionEntity(subscriptionReference(payment.getSuscripcionId()));
        entity.setPlan(payment.getPlan());
        entity.setPeriodo(payment.getPeriodo());
        entity.setMonto(payment.getMonto());
        entity.setMoneda(payment.getMoneda());
        entity.setEstado(payment.getEstado());
        entity.setCreadoEn(payment.getCreadoEn());
        entity.setAprobadoEn(payment.getAprobadoEn());
        return entity;
    }

    private UsuarioEntity userReference(UUID userId) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setId(userId);
        return entity;
    }

    private SuscripcionUsuarioEntity subscriptionReference(UUID subscriptionId) {
        if (subscriptionId == null) {
            return null;
        }

        SuscripcionUsuarioEntity entity = new SuscripcionUsuarioEntity();
        entity.setId(subscriptionId);
        return entity;
    }
}
