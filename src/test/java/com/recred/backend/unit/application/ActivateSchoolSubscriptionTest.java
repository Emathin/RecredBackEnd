package com.recred.backend.unit.application;

import com.recred.backend.application.ActivateSchoolSubscription;
import com.recred.backend.application.ActivateSchoolSubscriptionRequest;
import com.recred.backend.application.ActivateSchoolSubscriptionResponse;
import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.domain.models.SuscripcionColegio;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.SuscripcionColegioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivateSchoolSubscriptionTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-05T13:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private ColegioRepository schoolRepository;

    @Mock
    private SuscripcionColegioRepository subscriptionRepository;

    private ActivateSchoolSubscription useCase;

    private UUID schoolId;
    private UUID subscriptionId;
    private SuscripcionColegio existingSubscription;
    private ActivateSchoolSubscriptionResponse response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new ActivateSchoolSubscription(
                schoolRepository,
                subscriptionRepository,
                CLOCK
        );
    }

    @Test
    void givenActiveSchoolWithoutSubscription_whenActivatingSubscription_thenCreatesSubscription() {
        givenSchoolSubscriptionScenario();
        givenActiveSchool();
        givenNoActiveSubscription();
        givenSavedSubscriptionWithId();

        whenActivatingSchoolSubscription();

        thenSubscriptionWasActivatedForSchool();
        thenSubscriptionDatesStartAtFixedNow();
    }

    @Test
    void givenActiveSchoolWithSubscription_whenActivatingSubscription_thenRenewsSubscription() {
        givenSchoolSubscriptionScenario();
        givenExpiredActiveSubscription();
        givenActiveSchool();
        givenActiveSubscriptionExists();
        givenSavedSubscriptionWithId();

        whenActivatingSchoolSubscription();

        thenExistingSubscriptionWasRenewed();
        thenSubscriptionDatesStartAtFixedNow();
    }

    @Test
    void givenSchoolDoesNotExist_whenActivatingSubscription_thenFailsBeforeSaving() {
        givenSchoolSubscriptionScenario();
        givenSchoolDoesNotExist();

        whenActivatingSchoolSubscriptionFails();

        thenSchoolWasNotFound();
        thenSubscriptionWasNotSaved();
    }

    private void givenSchoolSubscriptionScenario() {
        schoolId = UUID.randomUUID();
        subscriptionId = UUID.randomUUID();
    }

    private void givenActiveSchool() {
        when(schoolRepository.existsActiveById(schoolId)).thenReturn(true);
    }

    private void givenSchoolDoesNotExist() {
        when(schoolRepository.existsActiveById(schoolId)).thenReturn(false);
    }

    private void givenNoActiveSubscription() {
        when(subscriptionRepository.findActiveByColegioId(schoolId)).thenReturn(Optional.empty());
    }

    private void givenExpiredActiveSubscription() {
        existingSubscription = SuscripcionColegio.activate(
                schoolId,
                new BigDecimal("20.00"),
                "USD",
                fixedNow().minusMonths(2)
        );
        existingSubscription.setId(subscriptionId);
    }

    private void givenActiveSubscriptionExists() {
        when(subscriptionRepository.findActiveByColegioId(schoolId))
                .thenReturn(Optional.of(existingSubscription));
    }

    private void givenSavedSubscriptionWithId() {
        when(subscriptionRepository.save(any(SuscripcionColegio.class))).thenAnswer(invocation -> {
            SuscripcionColegio subscription = invocation.getArgument(0);
            if (subscription.getId() == null) {
                subscription.setId(subscriptionId);
            }
            return subscription;
        });
    }

    private void whenActivatingSchoolSubscription() {
        response = useCase.execute(new ActivateSchoolSubscriptionRequest(
                schoolId,
                new BigDecimal("20.00"),
                "USD",
                null
        ));
    }

    private void whenActivatingSchoolSubscriptionFails() {
        thrownException = assertThrows(
                RuntimeException.class,
                this::whenActivatingSchoolSubscription);
    }

    private void thenSubscriptionWasActivatedForSchool() {
        assertEquals(subscriptionId, response.subscriptionId());
        assertEquals(schoolId, response.colegioId());
        assertEquals(EstadoSuscripcion.ACTIVA, response.estado());
    }

    private void thenExistingSubscriptionWasRenewed() {
        SuscripcionColegio savedSubscription = savedSubscription();
        assertEquals(subscriptionId, response.subscriptionId());
        assertEquals(subscriptionId, savedSubscription.getId());
        assertEquals(EstadoSuscripcion.ACTIVA, savedSubscription.getEstado());
    }

    private void thenSubscriptionDatesStartAtFixedNow() {
        assertEquals(fixedNow(), response.fechaInicio());
        assertEquals(fixedNow().plusMonths(1), response.fechaVencimiento());
        assertEquals(fixedNow().plusMonths(2), response.fechaGraciaHasta());
    }

    private void thenSchoolWasNotFound() {
        assertTrue(thrownException instanceof SchoolNotFoundException);
    }

    private void thenSubscriptionWasNotSaved() {
        verify(subscriptionRepository, never()).save(any(SuscripcionColegio.class));
    }

    private SuscripcionColegio savedSubscription() {
        ArgumentCaptor<SuscripcionColegio> captor =
                ArgumentCaptor.forClass(SuscripcionColegio.class);
        verify(subscriptionRepository, times(1)).save(captor.capture());
        return captor.getValue();
    }

    private LocalDateTime fixedNow() {
        return LocalDateTime.now(CLOCK);
    }
}
