package com.recred.backend.unit.infrastructure.scheduling;

import com.recred.backend.application.GenerateUserSubscriptionExpirationWarnings;
import com.recred.backend.application.GenerateUserSubscriptionExpirationWarningsRequest;
import com.recred.backend.infrastructure.scheduling.UserSubscriptionExpirationWarningJob;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UserSubscriptionExpirationWarningJobTest {

    @Test
    void run_ShouldDelegateToUseCaseWithCurrentTime() {
        GenerateUserSubscriptionExpirationWarnings useCase =
                mock(GenerateUserSubscriptionExpirationWarnings.class);
        Clock clock = fixedClock();
        UserSubscriptionExpirationWarningJob job =
                new UserSubscriptionExpirationWarningJob(useCase, clock);

        job.run();

        GenerateUserSubscriptionExpirationWarningsRequest request = capturedRequest(useCase);
        assertEquals(LocalDateTime.now(clock), request.targetTime());
    }

    private GenerateUserSubscriptionExpirationWarningsRequest capturedRequest(
            GenerateUserSubscriptionExpirationWarnings useCase
    ) {
        ArgumentCaptor<GenerateUserSubscriptionExpirationWarningsRequest> captor =
                ArgumentCaptor.forClass(GenerateUserSubscriptionExpirationWarningsRequest.class);
        verify(useCase).execute(captor.capture());
        return captor.getValue();
    }

    private Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-07-05T13:00:00Z"), ZoneOffset.UTC);
    }
}
