package com.recred.backend.infrastructure.scheduling;

import com.recred.backend.application.ExpireUserSubscriptions;
import com.recred.backend.application.ExpireUserSubscriptionsRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class ExpireUserSubscriptionsJob {

    private final ExpireUserSubscriptions expireUserSubscriptions;
    private final Clock clock;

    public ExpireUserSubscriptionsJob(
            ExpireUserSubscriptions expireUserSubscriptions,
            Clock clock
    ) {
        this.expireUserSubscriptions = expireUserSubscriptions;
        this.clock = clock;
    }

    @Scheduled(cron = "${user-subscription.expiration.cron:0 5 0 * * *}")
    public void run() {
        expireUserSubscriptions.execute(
                new ExpireUserSubscriptionsRequest(LocalDateTime.now(clock))
        );
    }
}
