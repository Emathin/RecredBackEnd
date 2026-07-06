package com.recred.backend.unit.infrastructure.persistence;

import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.SuscripcionUsuario;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.infrastructure.entities.SuscripcionUsuarioEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.SpringDataSuscripcionUsuarioRepository;
import com.recred.backend.infrastructure.persistence.SuscripcionUsuarioRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuscripcionUsuarioRepositoryImplTest {

    private final SpringDataSuscripcionUsuarioRepository springDataRepository =
            mock(SpringDataSuscripcionUsuarioRepository.class);
    private final SuscripcionUsuarioRepositoryImpl repository =
            new SuscripcionUsuarioRepositoryImpl(springDataRepository);

    @Test
    void findActiveByUsuarioId_ShouldReturnActiveSubscription() {
        UUID userId = UUID.randomUUID();
        SuscripcionUsuarioEntity entity = entity(UUID.randomUUID(), userId);
        givenActiveSubscription(userId, entity);

        Optional<SuscripcionUsuario> result = whenFindingActiveByUsuarioId(userId);

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
        verify(springDataRepository).findFirstByUsuarioEntityIdAndEstadoOrderByFechaInicioDesc(
                userId,
                EstadoSuscripcion.ACTIVA
        );
    }

    @Test
    void findByUsuarioId_ShouldReturnHistoryOrderedByStartDate() {
        UUID userId = UUID.randomUUID();
        SuscripcionUsuarioEntity entity = entity(UUID.randomUUID(), userId);
        givenUserSubscriptions(userId, List.of(entity));

        List<SuscripcionUsuario> result = whenFindingByUsuarioId(userId);

        assertEquals(1, result.size());
        assertEquals(entity.getId(), result.get(0).getId());
        verify(springDataRepository).findByUsuarioEntityIdOrderByFechaInicioDesc(userId);
    }

    @Test
    void findActiveExpiredAt_ShouldReturnExpiredActiveSubscriptions() {
        UUID userId = UUID.randomUUID();
        LocalDateTime referenceDate = startDate().plusMonths(1);
        SuscripcionUsuarioEntity entity = entity(UUID.randomUUID(), userId);
        givenExpiredSubscriptions(referenceDate, List.of(entity));

        List<SuscripcionUsuario> result = whenFindingActiveExpiredAt(referenceDate);

        assertEquals(1, result.size());
        assertEquals(entity.getId(), result.get(0).getId());
        verify(springDataRepository)
                .findByEstadoAndFechaVencimientoLessThanEqualOrderByFechaVencimientoAsc(
                        EstadoSuscripcion.ACTIVA,
                        referenceDate
                );
    }

    @Test
    void findActiveExpiringBetween_ShouldReturnExpiringSubscriptions() {
        UUID userId = UUID.randomUUID();
        LocalDateTime from = startDate();
        LocalDateTime to = from.plusDays(7);
        SuscripcionUsuarioEntity entity = entity(UUID.randomUUID(), userId);
        givenExpiringSubscriptions(from, to, List.of(entity));

        List<SuscripcionUsuario> result = whenFindingActiveExpiringBetween(from, to);

        assertEquals(1, result.size());
        assertEquals(entity.getId(), result.get(0).getId());
        verify(springDataRepository)
                .findByEstadoAndFechaVencimientoBetweenOrderByFechaVencimientoAsc(
                        EstadoSuscripcion.ACTIVA,
                        from,
                        to
                );
    }

    @Test
    void save_ShouldPersistSubscriptionWithUserReference() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SuscripcionUsuario subscription = subscription(userId);
        givenSavedEntity(entity(subscriptionId, userId));

        SuscripcionUsuario result = whenSaving(subscription);

        SuscripcionUsuarioEntity savedEntity = capturedSavedEntity();
        assertEquals(subscriptionId, result.getId());
        assertEquals(userId, savedEntity.getUsuarioEntity().getId());
        assertEquals(TipoPlan.INTERMEDIO, savedEntity.getPlan());
        assertEquals(EstadoSuscripcion.ACTIVA, savedEntity.getEstado());
        assertEquals(subscription.getUltimoAvisoVencimientoEn(),
                savedEntity.getUltimoAvisoVencimientoEn());
    }

    private void givenActiveSubscription(UUID userId, SuscripcionUsuarioEntity entity) {
        when(springDataRepository.findFirstByUsuarioEntityIdAndEstadoOrderByFechaInicioDesc(
                userId,
                EstadoSuscripcion.ACTIVA
        )).thenReturn(Optional.of(entity));
    }

    private void givenUserSubscriptions(
            UUID userId,
            List<SuscripcionUsuarioEntity> subscriptions
    ) {
        when(springDataRepository.findByUsuarioEntityIdOrderByFechaInicioDesc(userId))
                .thenReturn(subscriptions);
    }

    private void givenExpiredSubscriptions(
            LocalDateTime referenceDate,
            List<SuscripcionUsuarioEntity> subscriptions
    ) {
        when(springDataRepository
                .findByEstadoAndFechaVencimientoLessThanEqualOrderByFechaVencimientoAsc(
                        EstadoSuscripcion.ACTIVA,
                        referenceDate
                )).thenReturn(subscriptions);
    }

    private void givenExpiringSubscriptions(
            LocalDateTime from,
            LocalDateTime to,
            List<SuscripcionUsuarioEntity> subscriptions
    ) {
        when(springDataRepository
                .findByEstadoAndFechaVencimientoBetweenOrderByFechaVencimientoAsc(
                        EstadoSuscripcion.ACTIVA,
                        from,
                        to
                )).thenReturn(subscriptions);
    }

    private void givenSavedEntity(SuscripcionUsuarioEntity entity) {
        when(springDataRepository.saveAndFlush(any(SuscripcionUsuarioEntity.class)))
                .thenReturn(entity);
    }

    private Optional<SuscripcionUsuario> whenFindingActiveByUsuarioId(UUID userId) {
        return repository.findActiveByUsuarioId(userId);
    }

    private List<SuscripcionUsuario> whenFindingByUsuarioId(UUID userId) {
        return repository.findByUsuarioId(userId);
    }

    private List<SuscripcionUsuario> whenFindingActiveExpiredAt(LocalDateTime referenceDate) {
        return repository.findActiveExpiredAt(referenceDate);
    }

    private List<SuscripcionUsuario> whenFindingActiveExpiringBetween(
            LocalDateTime from,
            LocalDateTime to
    ) {
        return repository.findActiveExpiringBetween(from, to);
    }

    private SuscripcionUsuario whenSaving(SuscripcionUsuario subscription) {
        return repository.save(subscription);
    }

    private SuscripcionUsuarioEntity capturedSavedEntity() {
        ArgumentCaptor<SuscripcionUsuarioEntity> captor =
                ArgumentCaptor.forClass(SuscripcionUsuarioEntity.class);
        verify(springDataRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private SuscripcionUsuario subscription(UUID userId) {
        SuscripcionUsuario subscription = SuscripcionUsuario.activate(
                userId,
                TipoPlan.INTERMEDIO,
                PeriodoSuscripcion.MENSUAL,
                new BigDecimal("5500.00"),
                startDate()
        );
        subscription.markExpirationWarningSentAt(startDate().plusDays(24));
        return subscription;
    }

    private SuscripcionUsuarioEntity entity(UUID subscriptionId, UUID userId) {
        SuscripcionUsuarioEntity entity = new SuscripcionUsuarioEntity();
        entity.setId(subscriptionId);
        entity.setUsuarioEntity(userEntity(userId));
        entity.setPlan(TipoPlan.INTERMEDIO);
        entity.setPeriodo(PeriodoSuscripcion.MENSUAL);
        entity.setPrecio(new BigDecimal("5500.00"));
        entity.setEstado(EstadoSuscripcion.ACTIVA);
        entity.setFechaInicio(startDate());
        entity.setFechaVencimiento(startDate().plusMonths(1));
        return entity;
    }

    private UsuarioEntity userEntity(UUID userId) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setId(userId);
        return entity;
    }

    private LocalDateTime startDate() {
        return LocalDateTime.of(2026, 7, 5, 10, 0);
    }
}
