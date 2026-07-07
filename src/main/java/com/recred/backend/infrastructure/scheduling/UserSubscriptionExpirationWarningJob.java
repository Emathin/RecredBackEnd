package com.recred.backend.infrastructure.scheduling;

import com.recred.backend.application.GenerateUserSubscriptionExpirationWarnings;
import com.recred.backend.application.GenerateUserSubscriptionExpirationWarningsRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class UserSubscriptionExpirationWarningJob {

    private final GenerateUserSubscriptionExpirationWarnings generateWarnings;
    private final Clock clock;

    public UserSubscriptionExpirationWarningJob(
            GenerateUserSubscriptionExpirationWarnings generateWarnings,
            Clock clock
    ) {
        this.generateWarnings = generateWarnings;
        this.clock = clock;
    }

    @Scheduled(cron = "${user-subscription.warning.cron:0 10 9 * * *}")
    public void run() {
        generateWarnings.execute(
                new GenerateUserSubscriptionExpirationWarningsRequest(LocalDateTime.now(clock))
        );
    }
}
