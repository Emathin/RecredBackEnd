package com.recred.backend.unit.infrastructure.persistence;

import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.domain.models.SuscripcionColegio;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.SuscripcionColegioEntity;
import com.recred.backend.infrastructure.persistence.SpringDataSuscripcionColegioRepository;
import com.recred.backend.infrastructure.persistence.SuscripcionColegioRepositoryImpl;
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

class SuscripcionColegioRepositoryImplTest {

    private final SpringDataSuscripcionColegioRepository springDataRepository =
            mock(SpringDataSuscripcionColegioRepository.class);
    private final SuscripcionColegioRepositoryImpl repository =
            new SuscripcionColegioRepositoryImpl(springDataRepository);

    @Test
    void findActiveByColegioId_ShouldReturnActiveSubscription() {
        UUID schoolId = UUID.randomUUID();
        SuscripcionColegioEntity entity = entity(UUID.randomUUID(), schoolId);
        givenActiveSubscription(schoolId, entity);

        Optional<SuscripcionColegio> result = whenFindingActiveByColegioId(schoolId);

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
        verify(springDataRepository).findFirstByColegioEntityIdAndEstadoOrderByFechaInicioDesc(
                schoolId,
                EstadoSuscripcion.ACTIVA
        );
    }

    @Test
    void findByColegioId_ShouldReturnHistoryOrderedByStartDate() {
        UUID schoolId = UUID.randomUUID();
        SuscripcionColegioEntity entity = entity(UUID.randomUUID(), schoolId);
        givenSchoolSubscriptions(schoolId, List.of(entity));

        List<SuscripcionColegio> result = whenFindingByColegioId(schoolId);

        assertEquals(1, result.size());
        assertEquals(entity.getId(), result.get(0).getId());
        verify(springDataRepository).findByColegioEntityIdOrderByFechaInicioDesc(schoolId);
    }

    @Test
    void save_ShouldPersistSubscriptionWithSchoolReference() {
        UUID subscriptionId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        SuscripcionColegio subscription = subscription(schoolId);
        givenSavedEntity(entity(subscriptionId, schoolId));

        SuscripcionColegio result = whenSaving(subscription);

        SuscripcionColegioEntity savedEntity = capturedSavedEntity();
        assertEquals(subscriptionId, result.getId());
        assertEquals(schoolId, savedEntity.getColegioEntity().getId());
        assertEquals(new BigDecimal("20.00"), savedEntity.getPrecio());
        assertEquals(EstadoSuscripcion.ACTIVA, savedEntity.getEstado());
    }

    private void givenActiveSubscription(UUID schoolId, SuscripcionColegioEntity entity) {
        when(springDataRepository.findFirstByColegioEntityIdAndEstadoOrderByFechaInicioDesc(
                schoolId,
                EstadoSuscripcion.ACTIVA
        )).thenReturn(Optional.of(entity));
    }

    private void givenSchoolSubscriptions(
            UUID schoolId,
            List<SuscripcionColegioEntity> subscriptions
    ) {
        when(springDataRepository.findByColegioEntityIdOrderByFechaInicioDesc(schoolId))
                .thenReturn(subscriptions);
    }

    private void givenSavedEntity(SuscripcionColegioEntity entity) {
        when(springDataRepository.save(any(SuscripcionColegioEntity.class)))
                .thenReturn(entity);
    }

    private Optional<SuscripcionColegio> whenFindingActiveByColegioId(UUID schoolId) {
        return repository.findActiveByColegioId(schoolId);
    }

    private List<SuscripcionColegio> whenFindingByColegioId(UUID schoolId) {
        return repository.findByColegioId(schoolId);
    }

    private SuscripcionColegio whenSaving(SuscripcionColegio subscription) {
        return repository.save(subscription);
    }

    private SuscripcionColegioEntity capturedSavedEntity() {
        ArgumentCaptor<SuscripcionColegioEntity> captor =
                ArgumentCaptor.forClass(SuscripcionColegioEntity.class);
        verify(springDataRepository).save(captor.capture());
        return captor.getValue();
    }

    private SuscripcionColegio subscription(UUID schoolId) {
        return SuscripcionColegio.activate(
                schoolId,
                new BigDecimal("20.00"),
                "USD",
                startDate()
        );
    }

    private SuscripcionColegioEntity entity(UUID subscriptionId, UUID schoolId) {
        SuscripcionColegioEntity entity = new SuscripcionColegioEntity();
        entity.setId(subscriptionId);
        entity.setColegioEntity(schoolEntity(schoolId));
        entity.setPrecio(new BigDecimal("20.00"));
        entity.setMoneda("USD");
        entity.setEstado(EstadoSuscripcion.ACTIVA);
        entity.setFechaInicio(startDate());
        entity.setFechaVencimiento(startDate().plusMonths(1));
        entity.setFechaGraciaHasta(startDate().plusMonths(2));
        return entity;
    }

    private ColegioEntity schoolEntity(UUID schoolId) {
        ColegioEntity entity = new ColegioEntity();
        entity.setId(schoolId);
        return entity;
    }

    private LocalDateTime startDate() {
        return LocalDateTime.of(2026, 7, 5, 10, 0);
    }
}
