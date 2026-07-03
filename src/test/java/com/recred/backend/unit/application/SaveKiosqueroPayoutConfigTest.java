package com.recred.backend.unit.application;

import com.recred.backend.application.SaveKiosqueroPayoutConfig;
import com.recred.backend.application.SaveKiosqueroPayoutConfigCommand;
import com.recred.backend.application.SaveKiosqueroPayoutConfigResponse;
import com.recred.backend.domain.models.IntervalUnit;
import com.recred.backend.domain.models.KiosqueroPayoutConfig;
import com.recred.backend.domain.models.PayoutScheduleStatus;
import com.recred.backend.domain.repositories.KiosqueroPayoutConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveKiosqueroPayoutConfigTest {

    @Mock
    private KiosqueroPayoutConfigRepository repository;

    private SaveKiosqueroPayoutConfig useCase;

    @BeforeEach
    void setUp() {
        useCase = new SaveKiosqueroPayoutConfig(repository);
    }

    @Test
    void execute_WithValidCommand_ShouldSaveConfig() {
        UUID kiosqueroId = UUID.randomUUID();
        SaveKiosqueroPayoutConfigCommand command = new SaveKiosqueroPayoutConfigCommand(
                kiosqueroId, "1234567890123456789012", "20123456789", "John Doe",
                1, IntervalUnit.MONTHS, PayoutScheduleStatus.ACTIVE);

        KiosqueroPayoutConfig savedConfig = new KiosqueroPayoutConfig();
        savedConfig.setKiosqueroId(kiosqueroId);
        savedConfig.setDestinationCvu("1234567890123456789012");
        savedConfig.setDestinationCuit("20123456789");
        savedConfig.setAccountHolderName("John Doe");
        savedConfig.setCantidadIntervalo(1);
        savedConfig.setUnidadIntervalo(IntervalUnit.MONTHS);
        savedConfig.setEstado(PayoutScheduleStatus.ACTIVE);

        when(repository.save(any(KiosqueroPayoutConfig.class))).thenReturn(savedConfig);

        SaveKiosqueroPayoutConfigResponse response = useCase.execute(command);

        assertNotNull(response);
        assertEquals(kiosqueroId, response.kiosqueroId());

        ArgumentCaptor<KiosqueroPayoutConfig> captor = ArgumentCaptor.forClass(KiosqueroPayoutConfig.class);
        verify(repository).save(captor.capture());

        KiosqueroPayoutConfig captured = captor.getValue();
        assertEquals(kiosqueroId, captured.getKiosqueroId());
        assertEquals("1234567890123456789012", captured.getDestinationCvu());
        assertNotNull(captured.getProximaEjecucion());
    }
}
