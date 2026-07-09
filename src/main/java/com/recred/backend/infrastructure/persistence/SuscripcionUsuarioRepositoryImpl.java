package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.SuscripcionUsuario;
import com.recred.backend.domain.repositories.SuscripcionUsuarioRepository;
import com.recred.backend.infrastructure.entities.SuscripcionUsuarioEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SuscripcionUsuarioRepositoryImpl implements SuscripcionUsuarioRepository {

    private final SpringDataSuscripcionUsuarioRepository springDataRepository;

    public SuscripcionUsuarioRepositoryImpl(
            SpringDataSuscripcionUsuarioRepository springDataRepository
    ) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SuscripcionUsuario> findActiveByUsuarioId(UUID usuarioId) {
        return springDataRepository
                .findFirstByUsuarioEntityIdAndEstadoOrderByFechaInicioDesc(
                        usuarioId,
                        EstadoSuscripcion.ACTIVA)
                .map(SuscripcionUsuarioEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SuscripcionUsuario> findByUsuarioId(UUID usuarioId) {
        return springDataRepository.findByUsuarioEntityIdOrderByFechaInicioDesc(usuarioId)
                .stream()
                .map(SuscripcionUsuarioEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SuscripcionUsuario> findActiveExpiredAt(LocalDateTime referenceDate) {
        return springDataRepository
                .findByEstadoAndFechaVencimientoLessThanEqualOrderByFechaVencimientoAsc(
                        EstadoSuscripcion.ACTIVA,
                        referenceDate)
                .stream()
                .map(SuscripcionUsuarioEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SuscripcionUsuario> findActiveExpiringBetween(
            LocalDateTime from,
            LocalDateTime to
    ) {
        return springDataRepository
                .findByEstadoAndFechaVencimientoBetweenOrderByFechaVencimientoAsc(
                        EstadoSuscripcion.ACTIVA,
                        from,
                        to)
                .stream()
                .map(SuscripcionUsuarioEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public SuscripcionUsuario save(SuscripcionUsuario subscription) {
        SuscripcionUsuarioEntity entity = existingEntity(subscription);
        SuscripcionUsuarioEntity saved = springDataRepository.saveAndFlush(
                toEntity(subscription, entity));
        return saved.toDomain();
    }

    private SuscripcionUsuarioEntity existingEntity(SuscripcionUsuario subscription) {
        if (subscription.getId() == null) {
            return new SuscripcionUsuarioEntity();
        }

        return springDataRepository.findById(subscription.getId())
                .orElse(new SuscripcionUsuarioEntity());
    }

    private SuscripcionUsuarioEntity toEntity(
            SuscripcionUsuario subscription,
            SuscripcionUsuarioEntity entity
    ) {
        if (entity.getId() == null) {
            entity.setId(subscription.getId());
        }

        entity.setUsuarioEntity(userReference(subscription.getUsuarioId()));
        entity.setPlan(subscription.getPlan());
        entity.setPeriodo(subscription.getPeriodo());
        entity.setPrecio(subscription.getPrecio());
        entity.setEstado(subscription.getEstado());
        entity.setFechaInicio(subscription.getFechaInicio());
        entity.setFechaVencimiento(subscription.getFechaVencimiento());
        entity.setUltimoAvisoVencimientoEn(subscription.getUltimoAvisoVencimientoEn());
        entity.setCreadoEn(subscription.getCreadoEn());
        entity.setActualizadoEn(subscription.getActualizadoEn());
        return entity;
    }

    private UsuarioEntity userReference(UUID usuarioId) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setId(usuarioId);
        return entity;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUsedTrial(UUID userId) {
        return findByUsuarioId(userId).stream()
                .anyMatch(sub -> sub.getPeriodo() == PeriodoSuscripcion.PRUEBA);
    }
}
