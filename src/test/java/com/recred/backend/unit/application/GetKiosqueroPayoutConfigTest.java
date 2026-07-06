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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetKiosqueroPayoutConfigTest {

    @Mock
    private KiosqueroPayoutConfigRepository repository;

    private GetKiosqueroPayoutConfig useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetKiosqueroPayoutConfig(repository);
    }

    @Test
    void execute_WhenConfigExists_ShouldReturnResponse() {
        UUID kiosqueroId = UUID.randomUUID();
        GetKiosqueroPayoutConfigRequest request = new GetKiosqueroPayoutConfigRequest(kiosqueroId);

        KiosqueroPayoutConfig config = new KiosqueroPayoutConfig();
        config.setKiosqueroId(kiosqueroId);
        config.setDestinationCvu("cvu123");
        config.setDestinationCuit("cuit123");
        config.setAccountHolderName("John");
        config.setCantidadIntervalo(1);
        config.setUnidadIntervalo(IntervalUnit.DAYS);
        config.setEstado(PayoutScheduleStatus.ACTIVE);

        when(repository.findByKiosqueroId(kiosqueroId)).thenReturn(Optional.of(config));

        GetKiosqueroPayoutConfigResponse response = useCase.execute(request);

        assertNotNull(response);
        assertEquals(kiosqueroId, response.kiosqueroId());
        assertEquals("cvu123", response.destinationCvu());
    }

    @Test
    void execute_WhenConfigDoesNotExist_ShouldThrowException() {
        UUID kiosqueroId = UUID.randomUUID();
        GetKiosqueroPayoutConfigRequest request = new GetKiosqueroPayoutConfigRequest(kiosqueroId);

        when(repository.findByKiosqueroId(kiosqueroId)).thenReturn(Optional.empty());

        assertThrows(KiosqueroPayoutConfigNotFoundException.class, () -> useCase.execute(request));
    }
}
