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

    @Test
    void fromDomainShouldMapAlertValues() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID purchaseId = UUID.randomUUID();
        LocalDateTime occurredAt = LocalDateTime.of(2026, 7, 4, 12, 0);

        KiosqueroOperationalAlertResponse response =
                KiosqueroOperationalAlertResponse.fromDomain(
                        new KiosqueroOperationalAlert(
                                buffetId,
                                OperationalAlertType.DAILY_CAPACITY_LOW,
                                OperationalAlertSeverity.WARNING,
                                productId,
                                purchaseId,
                                occurredAt,
                                "Queda poco cupo"
                        )
                );

        assertEquals(buffetId, response.buffetId());
        assertEquals("DAILY_CAPACITY_LOW", response.type());
        assertEquals("WARNING", response.severity());
        assertEquals(productId, response.productId());
        assertEquals(purchaseId, response.purchaseId());
        assertEquals(occurredAt, response.occurredAt());
        assertEquals("Queda poco cupo", response.message());
    }

    @Test
    void fromApplicationShouldMapAlertValues() {
        UUID buffetId = UUID.randomUUID();
        UUID purchaseId = UUID.randomUUID();
        LocalDateTime occurredAt = LocalDateTime.of(2026, 7, 4, 13, 0);

        KiosqueroOperationalAlertResponse response =
                KiosqueroOperationalAlertResponse.fromApplication(
                        new OperationalAlertResponse(
                                buffetId,
                                "ORDER_EXPIRED",
                                "CRITICAL",
                                null,
                                purchaseId,
                                occurredAt,
                                "Pedido vencido"
                        )
                );

        assertEquals(buffetId, response.buffetId());
        assertEquals("ORDER_EXPIRED", response.type());
        assertEquals("CRITICAL", response.severity());
        assertEquals(purchaseId, response.purchaseId());
        assertEquals(occurredAt, response.occurredAt());
        assertEquals("Pedido vencido", response.message());
    }
}
