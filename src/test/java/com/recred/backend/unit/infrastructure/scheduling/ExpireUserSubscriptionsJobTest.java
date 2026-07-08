package com.recred.backend.unit.infrastructure.scheduling;

import com.recred.backend.application.ExpireUserSubscriptions;
import com.recred.backend.application.ExpireUserSubscriptionsRequest;
import com.recred.backend.infrastructure.scheduling.ExpireUserSubscriptionsJob;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ExpireUserSubscriptionsJobTest {

    @Test
    void run_ShouldDelegateToUseCaseWithCurrentTime() {
        ExpireUserSubscriptions useCase = mock(ExpireUserSubscriptions.class);
        Clock clock = fixedClock();
        ExpireUserSubscriptionsJob job = new ExpireUserSubscriptionsJob(useCase, clock);

        job.run();

        ExpireUserSubscriptionsRequest request = capturedRequest(useCase);
        assertEquals(LocalDateTime.now(clock), request.targetTime());
    }

    private ExpireUserSubscriptionsRequest capturedRequest(ExpireUserSubscriptions useCase) {
        ArgumentCaptor<ExpireUserSubscriptionsRequest> captor =
                ArgumentCaptor.forClass(ExpireUserSubscriptionsRequest.class);
        verify(useCase).execute(captor.capture());
        return captor.getValue();
    }

    private Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-07-05T13:00:00Z"), ZoneOffset.UTC);
    }
}
