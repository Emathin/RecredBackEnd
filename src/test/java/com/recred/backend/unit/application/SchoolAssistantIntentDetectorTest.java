package com.recred.backend.application;

import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                detector.detectIntent("para el 3/6"));
        assertEquals(SchoolAssistantIntent.EVENTS,
                detector.detectIntent("eventos del 3/6"));
        assertEquals(LocalDate.of(2026, 6, 1),
                detector.purchaseDate("lunes", today).orElseThrow());
        assertEquals(LocalDate.of(2026, 6, 3),
                detector.purchaseDate("3/6", today).orElseThrow());
    }

    @Test
    void purchaseDateShouldDetectTodayDayAfterTomorrowAndPastDateWithoutYear() {
        LocalDate today = LocalDate.of(2026, 5, 27);

        assertEquals(LocalDate.of(2026, 5, 27),
                detector.purchaseDate("hoy", today).orElseThrow());
        assertEquals(LocalDate.of(2027, 1, 1),
                detector.purchaseDate("1/1", today).orElseThrow());
        assertTrue(detector.purchaseDate("31/2", today).isEmpty());
    }

    @Test
    void purchaseDateShouldDetectWeekdayNames() {
        LocalDate today = LocalDate.of(2026, 5, 27);

        assertEquals(LocalDate.of(2026, 6, 2),
                detector.purchaseDate("martes", today).orElseThrow());
        assertEquals(LocalDate.of(2026, 5, 27),
                detector.purchaseDate("miercoles", today).orElseThrow());
        assertEquals(LocalDate.of(2026, 5, 28),
                detector.purchaseDate("jueves", today).orElseThrow());
        assertEquals(LocalDate.of(2026, 5, 29),
                detector.purchaseDate("viernes", today).orElseThrow());
        assertEquals(LocalDate.of(2026, 5, 30),
                detector.purchaseDate("sabado", today).orElseThrow());
        assertEquals(LocalDate.of(2026, 5, 31),
                detector.purchaseDate("domingo", today).orElseThrow());
    }

}
