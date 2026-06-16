package com.recred.backend.integration.adapters.scheduling;

import com.recred.backend.application.GenerateLowBalanceAlerts;
import com.recred.backend.infrastructure.scheduling.LowBalanceAlertJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LowBalanceAlertJobTest {

    @Mock
    private GenerateLowBalanceAlerts generateLowBalanceAlerts;

    @InjectMocks
    private LowBalanceAlertJob lowBalanceAlertJob;

    @Test
    void execute_DeberiaLlamarAlCasoDeUso() {
        lowBalanceAlertJob.executeLowBalanceAlerts();

        verify(generateLowBalanceAlerts).execute();
    }
}
