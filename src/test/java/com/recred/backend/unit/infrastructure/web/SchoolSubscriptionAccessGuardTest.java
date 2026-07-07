package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.models.SuscripcionColegio;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.SuscripcionColegioRepository;
import com.recred.backend.infrastructure.web.SchoolSubscriptionAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolSubscriptionAccessGuardTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-05T13:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private ColegioRepository colegioRepository;

    @Mock
    private SuscripcionColegioRepository suscripcionColegioRepository;

    private SchoolSubscriptionAccessGuard guard;
    private UUID colegioId;

    @BeforeEach
    void setUp() {
        guard = new SchoolSubscriptionAccessGuard(
                colegioRepository,
                suscripcionColegioRepository,
                CLOCK
        );
        colegioId = UUID.randomUUID();
    }

    @Test
    void requireActiveSchoolSubscription_WhenPaidSubscriptionIsActive_ShouldAllowAccess() {
        givenActiveSchool();
        givenActiveSubscription(subscriptionStartedAt(now().minusDays(10)));

        Executable execution = () -> whenRequiringActiveSchoolSubscription();

        assertDoesNotThrow(execution);
    }

    @Test
    void requireActiveSchoolSubscription_WhenSubscriptionIsInGracePeriod_ShouldAllowAccess() {
        givenActiveSchool();
        givenActiveSubscription(subscriptionStartedAt(now().minusMonths(1).minusDays(5)));

        Executable execution = () -> whenRequiringActiveSchoolSubscription();

        assertDoesNotThrow(execution);
    }

    @Test
    void requireActiveSchoolSubscription_WhenSubscriptionDoesNotExist_ShouldDenyAccess() {
        givenActiveSchool();
        givenActiveSubscriptionDoesNotExist();

        Executable execution = () -> whenRequiringActiveSchoolSubscription();

        assertThrows(AccessDeniedException.class, execution);
    }

    @Test
    void requireActiveSchoolSubscription_WhenGracePeriodExpired_ShouldDenyAccess() {
        givenActiveSchool();
        givenActiveSubscription(subscriptionStartedAt(now().minusMonths(2)));

        Executable execution = () -> whenRequiringActiveSchoolSubscription();

        assertThrows(AccessDeniedException.class, execution);
    }

    @Test
    void requireActiveSchoolSubscription_WhenSchoolDoesNotExist_ShouldThrowException() {
        givenSchoolDoesNotExist();

        Executable execution = () -> whenRequiringActiveSchoolSubscription();

        assertThrows(SchoolNotFoundException.class, execution);
    }

    private void givenActiveSchool() {
        when(colegioRepository.existsActiveById(colegioId)).thenReturn(true);
    }

    private void givenSchoolDoesNotExist() {
        when(colegioRepository.existsActiveById(colegioId)).thenReturn(false);
    }

    private void givenActiveSubscription(SuscripcionColegio subscription) {
        when(suscripcionColegioRepository.findActiveByColegioId(colegioId))
                .thenReturn(Optional.of(subscription));
    }

    private void givenActiveSubscriptionDoesNotExist() {
        when(suscripcionColegioRepository.findActiveByColegioId(colegioId))
                .thenReturn(Optional.empty());
    }

    private void whenRequiringActiveSchoolSubscription() {
        guard.requireActiveSchoolSubscription(colegioId);
    }

    private SuscripcionColegio subscriptionStartedAt(LocalDateTime startDate) {
        return SuscripcionColegio.activate(
                colegioId,
                new BigDecimal("20.00"),
                "USD",
                startDate
        );
    }

    private LocalDateTime now() {
        return LocalDateTime.now(CLOCK);
    }
}
