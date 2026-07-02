package com.recred.backend.application;


import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SchoolAssistantIntentDetectorTest {

    private final SchoolAssistantIntentDetector detector = new SchoolAssistantIntentDetector();

    @Test
    void recessTimeShouldDetectNoonFromLunchMessage() {
        assertEquals(RecessTime.NOON, detector.recessTime("almuerzo").orElseThrow());
        assertEquals(SchoolAssistantIntent.SELECT_RECESS,
                detector.detectIntent("quiero retirar al mediodia"));
    }

    @Test
    void cancellationShouldDistinguishDraftFromCreatedPurchase() {
        assertEquals(SchoolAssistantIntent.CANCEL_ACTION,
                detector.detectIntent("cancelar"));
        assertEquals(SchoolAssistantIntent.CANCEL_PURCHASE,
                detector.detectIntent("cancelar mi compra"));
        assertEquals(SchoolAssistantIntent.CANCEL_PURCHASE,
                detector.detectIntent("anular pedido"));
    }

    @Test
    void purchaseDateShouldDetectRelativeWeekdayAndExplicitDates() {
        LocalDate today = LocalDate.of(2026, 5, 27);

        assertEquals(SchoolAssistantIntent.SELECT_PURCHASE_DATE,
                detector.detectIntent("mañana"));
        assertEquals(SchoolAssistantIntent.SELECT_PURCHASE_DATE,
                detector.detectIntent("para el 3/6"));
        assertEquals(SchoolAssistantIntent.EVENTS,
                detector.detectIntent("eventos del 3/6"));
        assertEquals(LocalDate.of(2026, 5, 28),
                detector.purchaseDate("mañana", today).orElseThrow());
        assertEquals(LocalDate.of(2026, 6, 1),
                detector.purchaseDate("lunes", today).orElseThrow());
        assertEquals(LocalDate.of(2026, 6, 3),
                detector.purchaseDate("3/6", today).orElseThrow());
    }
}
