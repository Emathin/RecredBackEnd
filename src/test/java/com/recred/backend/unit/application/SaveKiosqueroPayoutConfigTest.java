package com.recred.backend.unit.application;

import com.recred.backend.application.SaveKiosqueroPayoutConfig;
import com.recred.backend.application.SaveKiosqueroPayoutConfigRequest;
import com.recred.backend.application.SaveKiosqueroPayoutConfigResponse;
import com.recred.backend.domain.models.IntervalUnit;
import com.recred.backend.domain.models.KiosqueroPayoutConfig;
import com.recred.backend.domain.models.PayoutScheduleStatus;
import com.recred.backend.domain.repositories.KiosqueroPayoutConfigRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaveKiosqueroPayoutConfigTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-02T00:00:00Z"), ZoneOffset.UTC);
    private static final String DESTINATION_CVU = "1234567890123456789012";
    private static final String DESTINATION_CUIT = "20123456789";
    private static final String ACCOUNT_HOLDER_NAME = "John Doe";

    private final KiosqueroPayoutConfigRepository repository =
            mock(KiosqueroPayoutConfigRepository.class);
    private final SaveKiosqueroPayoutConfig useCase =
            new SaveKiosqueroPayoutConfig(repository, FIXED_CLOCK);

    private SaveKiosqueroPayoutConfigRequest request;
    private SaveKiosqueroPayoutConfigResponse response;
    private UUID kiosqueroId;

    @Test
    void execute_WhenConfigDoesNotExist_SavesNewPayoutConfig() {
        givenValidPayoutConfigRequest();
        givenConfigDoesNotExist();
        givenSavedConfigIsReturned();

        whenSavingPayoutConfig();

        thenResponseContainsSavedConfig();
        thenConfigWasSavedWithRequestData();
    }

    private void givenValidPayoutConfigRequest() {
        kiosqueroId = randomUUID();
        request = new SaveKiosqueroPayoutConfigRequest(
                kiosqueroId,
                DESTINATION_CVU,
                DESTINATION_CUIT,
                ACCOUNT_HOLDER_NAME,
                1,
                IntervalUnit.MONTHS,
                PayoutScheduleStatus.ACTIVE
        );
    }

    private void givenConfigDoesNotExist() {
        when(repository.findByKiosqueroId(kiosqueroId)).thenReturn(Optional.empty());
    }

    private void givenSavedConfigIsReturned() {
        when(repository.save(any(KiosqueroPayoutConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void whenSavingPayoutConfig() {
        response = useCase.execute(request);
    }

    private void thenResponseContainsSavedConfig() {
        assertEquals(kiosqueroId, response.kiosqueroId());
        assertEquals(DESTINATION_CVU, response.destinationCvu());
        assertEquals(LocalDate.of(2026, 8, 2), response.proximaEjecucion());
    }

    private void thenConfigWasSavedWithRequestData() {
        ArgumentCaptor<KiosqueroPayoutConfig> captor =
                ArgumentCaptor.forClass(KiosqueroPayoutConfig.class);
        verify(repository).save(captor.capture());

        KiosqueroPayoutConfig savedConfig = captor.getValue();
        assertEquals(kiosqueroId, savedConfig.getKiosqueroId());
        assertEquals(DESTINATION_CVU, savedConfig.getDestinationCvu());
        assertEquals(DESTINATION_CUIT, savedConfig.getDestinationCuit());
        assertEquals(ACCOUNT_HOLDER_NAME, savedConfig.getAccountHolderName());
        assertEquals(1, savedConfig.getCantidadIntervalo());
        assertEquals(IntervalUnit.MONTHS, savedConfig.getUnidadIntervalo());
        assertEquals(PayoutScheduleStatus.ACTIVE, savedConfig.getEstado());
        assertEquals(LocalDate.of(2026, 8, 2), savedConfig.getProximaEjecucion());
    }
}
