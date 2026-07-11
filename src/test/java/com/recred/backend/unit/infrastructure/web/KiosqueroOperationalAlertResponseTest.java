package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.OperationalAlertResponse;
import com.recred.backend.domain.models.KiosqueroOperationalAlert;
import com.recred.backend.domain.models.OperationalAlertSeverity;
import com.recred.backend.domain.models.OperationalAlertType;
import com.recred.backend.infrastructure.web.KiosqueroOperationalAlertResponse;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KiosqueroOperationalAlertResponseTest {

    private final UUID BUFFET_ID = UUID.randomUUID();
    private final UUID PRODUCT_ID = UUID.randomUUID();
    private final UUID PURCHASE_ID = UUID.randomUUID();
    private final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 7, 4, 12, 0);
    private final String MESSAGE = "Mensaje de prueba";
    private final String CRITICAL = "CRITICAL";

    @Test
    void fromDomainShouldMapAlertValues() {
        KiosqueroOperationalAlert alert = givenDomainAlert();
        
        KiosqueroOperationalAlertResponse response = whenMappingFromDomain(alert);
        
        thenDomainResponseIsMappedCorrectly(response);
    }

    @Test
    void fromApplicationShouldMapAlertValues() {
        OperationalAlertResponse alert = givenApplicationAlert();
        
        KiosqueroOperationalAlertResponse response = whenMappingFromApplication(alert);
        
        thenApplicationResponseIsMappedCorrectly(response);
    }

    private KiosqueroOperationalAlert givenDomainAlert() {
        return new KiosqueroOperationalAlert(
                BUFFET_ID,
                OperationalAlertType.DAILY_CAPACITY_LOW,
                OperationalAlertSeverity.WARNING,
                PRODUCT_ID,
                PURCHASE_ID,
                OCCURRED_AT,
                MESSAGE
        );
    }

    private OperationalAlertResponse givenApplicationAlert() {
        return new OperationalAlertResponse(
                BUFFET_ID,
                "ORDER_EXPIRED",
                CRITICAL,
                null,
                PURCHASE_ID,
                OCCURRED_AT,
                MESSAGE
        );
    }

    private KiosqueroOperationalAlertResponse whenMappingFromDomain(KiosqueroOperationalAlert alert) {
        return KiosqueroOperationalAlertResponse.fromDomain(alert);
    }

    private KiosqueroOperationalAlertResponse whenMappingFromApplication(OperationalAlertResponse alert) {
        return KiosqueroOperationalAlertResponse.fromApplication(alert);
    }

    private void thenDomainResponseIsMappedCorrectly(KiosqueroOperationalAlertResponse response) {
        assertEquals(BUFFET_ID, response.buffetId());
        assertEquals("DAILY_CAPACITY_LOW", response.type());
        assertEquals("WARNING", response.severity());
        assertEquals(PRODUCT_ID, response.productId());
        assertEquals(PURCHASE_ID, response.purchaseId());
        assertEquals(OCCURRED_AT, response.occurredAt());
        assertEquals(MESSAGE, response.message());
    }

    private void thenApplicationResponseIsMappedCorrectly(KiosqueroOperationalAlertResponse response) {
        assertEquals(BUFFET_ID, response.buffetId());
        assertEquals("ORDER_EXPIRED", response.type());
        assertEquals(CRITICAL, response.severity());
        assertEquals(PURCHASE_ID, response.purchaseId());
        assertEquals(OCCURRED_AT, response.occurredAt());
        assertEquals(MESSAGE, response.message());
    }
}
