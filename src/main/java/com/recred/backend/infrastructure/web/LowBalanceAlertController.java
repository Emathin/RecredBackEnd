package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GenerateLowBalanceAlerts;
import com.recred.backend.application.GenerateLowBalanceAlertsRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/low-balance-alerts")
public class LowBalanceAlertController {

    private final GenerateLowBalanceAlerts generateLowBalanceAlerts;

    public LowBalanceAlertController(GenerateLowBalanceAlerts generateLowBalanceAlerts) {
        this.generateLowBalanceAlerts = generateLowBalanceAlerts;
    }

    @PostMapping
    public ResponseEntity<String> triggerLowBalanceAlerts() {
        try {
            generateLowBalanceAlerts.execute(new GenerateLowBalanceAlertsRequest());
            return ResponseEntity.ok("Low balance alerts triggered successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error triggering low balance alerts: " + e.getMessage());
        }
    }
}
