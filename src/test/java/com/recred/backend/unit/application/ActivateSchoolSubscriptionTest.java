package com.recred.backend.unit.application;

import com.recred.backend.application.ActivateSchoolSubscription;
import com.recred.backend.application.ActivateSchoolSubscriptionRequest;
import com.recred.backend.application.ActivateSchoolSubscriptionResponse;
import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.domain.models.SuscripcionColegio;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.SuscripcionColegioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class ActivateSchoolSubscriptionTest {

    private final ColegioRepository colegioRepository = mock(ColegioRepository.class);
    private final SuscripcionColegioRepository subscriptionRepository =
            mock(SuscripcionColegioRepository.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-07-05T13:00:00Z"),
            ZoneOffset.UTC
    );
    private final ActivateSchoolSubscription useCase = new ActivateSchoolSubscription(
            colegioRepository,
            subscriptionRepository,
            clock
    );

    @Test
    void execute_WhenSchoolExists_ShouldActivateSubscription() {
        UUID schoolId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        givenActiveSchool(schoolId);
        givenNoActiveSubscription(schoolId);
        givenSavedSubscriptionWithId(subscriptionId);

        ActivateSchoolSubscriptionResponse result = whenExecuting(schoolId);

        assertEquals(subscriptionId, result.subscriptionId());
        assertEquals(schoolId, result.colegioId());
        assertEquals(fixedNow().plusMonths(1), result.fechaVencimiento());
        assertEquals(fixedNow().plusMonths(2), result.fechaGraciaHasta());
        assertEquals(EstadoSuscripcion.ACTIVA, result.estado());
    }

    @Test
    void execute_WhenSchoolHasActiveSubscription_ShouldRenewExistingSubscription() {
        UUID schoolId = UUID.randomUUID();
        UUID existingSubscriptionId = UUID.randomUUID();
        SuscripcionColegio existingSubscription = expiredActiveSubscription(
                schoolId,
                existingSubscriptionId);
        givenActiveSchool(schoolId);
        givenActiveSubscription(schoolId, existingSubscription);
        givenSavedSubscriptionWithId(UUID.randomUUID());

        ActivateSchoolSubscriptionResponse result = whenExecuting(schoolId);

        SuscripcionColegio savedSubscription = savedSubscription();
        assertEquals(existingSubscriptionId, result.subscriptionId());
        assertEquals(existingSubscriptionId, savedSubscription.getId());
        assertEquals(EstadoSuscripcion.ACTIVA, savedSubscription.getEstado());
        assertEquals(fixedNow(), savedSubscription.getFechaInicio());
        assertEquals(fixedNow().plusMonths(1), savedSubscription.getFechaVencimiento());
        assertEquals(fixedNow().plusMonths(2), savedSubscription.getFechaGraciaHasta());
    }

    @Test
    void execute_WhenSchoolDoesNotExist_ShouldThrowException() {
        UUID schoolId = UUID.randomUUID();
        givenMissingSchool(schoolId);

        assertThrows(SchoolNotFoundException.class, () -> whenExecuting(schoolId));
        verify(subscriptionRepository, never()).save(any(SuscripcionColegio.class));
    }

    private void givenActiveSchool(UUID schoolId) {
        when(colegioRepository.existsActiveById(schoolId)).thenReturn(true);
    }

    private void givenMissingSchool(UUID schoolId) {
        when(colegioRepository.existsActiveById(schoolId)).thenReturn(false);
    }

    private void givenNoActiveSubscription(UUID schoolId) {
        when(subscriptionRepository.findActiveByColegioId(schoolId)).thenReturn(Optional.empty());
    }

    private void givenActiveSubscription(UUID schoolId, SuscripcionColegio subscription) {
        when(subscriptionRepository.findActiveByColegioId(schoolId))
                .thenReturn(Optional.of(subscription));
    }

    private void givenSavedSubscriptionWithId(UUID subscriptionId) {
        when(subscriptionRepository.save(any(SuscripcionColegio.class))).thenAnswer(invocation -> {
            SuscripcionColegio subscription = invocation.getArgument(0);
            if (subscription.getId() == null) {
                subscription.setId(subscriptionId);
            }
            return subscription;
        });
    }

    private ActivateSchoolSubscriptionResponse whenExecuting(UUID schoolId) {
        return useCase.execute(new ActivateSchoolSubscriptionRequest(
                schoolId,
                new BigDecimal("20.00"),
                "USD",
                null
        ));
    }

    private SuscripcionColegio savedSubscription() {
        ArgumentCaptor<SuscripcionColegio> captor =
                ArgumentCaptor.forClass(SuscripcionColegio.class);
        verify(subscriptionRepository, times(1)).save(captor.capture());
        return captor.getValue();
    }

    private SuscripcionColegio expiredActiveSubscription(UUID schoolId, UUID subscriptionId) {
        SuscripcionColegio subscription = SuscripcionColegio.activate(
                schoolId,
                new BigDecimal("20.00"),
                "USD",
                fixedNow().minusMonths(2)
        );
        subscription.setId(subscriptionId);
        return subscription;
    }

    private LocalDateTime fixedNow() {
        return LocalDateTime.now(clock);
    }
}
