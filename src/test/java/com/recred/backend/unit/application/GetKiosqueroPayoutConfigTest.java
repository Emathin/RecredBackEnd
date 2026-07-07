package com.recred.backend.unit.application;

import com.recred.backend.application.GetKiosqueroPayoutConfig;
import com.recred.backend.application.GetKiosqueroPayoutConfigRequest;
import com.recred.backend.application.GetKiosqueroPayoutConfigResponse;
import com.recred.backend.domain.exceptions.KiosqueroPayoutConfigNotFoundException;
import com.recred.backend.domain.models.IntervalUnit;
import com.recred.backend.domain.models.KiosqueroPayoutConfig;
import com.recred.backend.domain.models.PayoutScheduleStatus;
import com.recred.backend.domain.repositories.KiosqueroPayoutConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetKiosqueroPayoutConfigTest {

    @Mock
    private KiosqueroPayoutConfigRepository repository;

    private GetKiosqueroPayoutConfig useCase;

    private UUID kiosqueroId;
    private KiosqueroPayoutConfig config;
    private GetKiosqueroPayoutConfigResponse response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new GetKiosqueroPayoutConfig(repository);
    }

    @Test
    void givenExistingPayoutConfig_whenGettingPayoutConfig_thenReturnsConfig() {
        givenPayoutConfigScenario();
        givenPayoutConfigExists();

        whenGettingPayoutConfig();

        thenPayoutConfigIsReturned();
    }

    @Test
    void givenMissingPayoutConfig_whenGettingPayoutConfig_thenFails() {
        givenPayoutConfigScenario();
        givenPayoutConfigDoesNotExist();

        whenGettingPayoutConfigFails();

        thenPayoutConfigNotFoundIsThrown();
    }

    private void givenPayoutConfigScenario() {
        kiosqueroId = UUID.randomUUID();
        config = new KiosqueroPayoutConfig();
        config.setKiosqueroId(kiosqueroId);
        config.setDestinationCvu("cvu123");
        config.setDestinationCuit("cuit123");
        config.setAccountHolderName("John");
        config.setCantidadIntervalo(1);
        config.setUnidadIntervalo(IntervalUnit.DAYS);
        config.setEstado(PayoutScheduleStatus.ACTIVE);
    }

    private void givenPayoutConfigExists() {
        when(repository.findByKiosqueroId(kiosqueroId)).thenReturn(Optional.of(config));
    }

    private void givenPayoutConfigDoesNotExist() {
        when(repository.findByKiosqueroId(kiosqueroId)).thenReturn(Optional.empty());
    }

    private void whenGettingPayoutConfig() {
        response = useCase.execute(new GetKiosqueroPayoutConfigRequest(kiosqueroId));
    }

    private void whenGettingPayoutConfigFails() {
        thrownException = assertThrows(RuntimeException.class, () ->
                useCase.execute(new GetKiosqueroPayoutConfigRequest(kiosqueroId)));
    }

    private void thenPayoutConfigIsReturned() {
        assertEquals(kiosqueroId, response.kiosqueroId());
        assertEquals("cvu123", response.destinationCvu());
        assertEquals("cuit123", response.destinationCuit());
        assertEquals("John", response.accountHolderName());
        assertEquals(1, response.cantidadIntervalo());
        assertEquals(IntervalUnit.DAYS, response.unidadIntervalo());
        assertEquals(PayoutScheduleStatus.ACTIVE, response.estado());
    }

    private void thenPayoutConfigNotFoundIsThrown() {
        assertTrue(thrownException instanceof KiosqueroPayoutConfigNotFoundException);
    }
}
