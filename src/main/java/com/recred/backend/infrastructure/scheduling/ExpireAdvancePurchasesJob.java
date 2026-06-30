package com.recred.backend.infrastructure.scheduling;

import com.recred.backend.application.ExpireAdvancePurchases;
import com.recred.backend.application.ExpireAdvancePurchasesRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class ExpireAdvancePurchasesJob {

    private final ExpireAdvancePurchases expireAdvancePurchases;
    private final Clock clock;

    public ExpireAdvancePurchasesJob(
            ExpireAdvancePurchases expireAdvancePurchases,
            Clock clock
    ) {
        this.expireAdvancePurchases = expireAdvancePurchases;
        this.clock = clock;
    }

    @Scheduled(cron = "${advance-purchase.expiration.cron:0 */10 * * * *}")
    public void run() {
        expireAdvancePurchases.execute(
                new ExpireAdvancePurchasesRequest(null, LocalDateTime.now(clock))
        );
    }
}
