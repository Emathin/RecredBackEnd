package com.recred.backend.unit.application;

import com.recred.backend.application.SchoolAssistantAnswerFormatter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SchoolAssistantAnswerFormatterTest {

    private final SchoolAssistantAnswerFormatter formatter = new SchoolAssistantAnswerFormatter();

    @Test
    void formatsSemicolonSeparatedAnswerAsBullets() {
        String answer = "Stock actual: Agua T\u00C3\u00B3nica 500ml por $1100.00 "
                + "(stock: 40); Caf\u00C3\u00A9 Espresso por $950.00 (stock: 40); "
                + "Sandwich de Jam\u00C3\u00B3n y Queso por $950.00 (stock: 25).";

        String formattedAnswer = formatter.format(answer);

        assertEquals("""
                Stock actual:
                - Agua T\u00F3nica 500ml por $1100.00 (stock: 40)
                - Caf\u00E9 Espresso por $950.00 (stock: 40)
                - Sandwich de Jam\u00F3n y Queso por $950.00 (stock: 25)
                """.trim(), formattedAnswer);
    }

    @Test
    void formatsTitledShortListsAsBullets() {
        assertEquals("""
                Puedo ayudarte con:
                - saldos
                - pagos
                - compras
                """.trim(), formatter.format("Puedo ayudarte con: saldos; pagos; compras."));
    }

    @Test
    void formatsUntitledListLikeItemsAsBullets() {
        assertEquals("""
                - Agua por $10
                - Jugo por $20
                - Pan por $30
                """.trim(), formatter.format("Agua por $10; Jugo por $20; Pan por $30"));
    }

    @Test
    void keepsShortAnswerWithoutListFormat() {
        assertEquals(
                "Tenes $500.00 disponibles.",
                formatter.format("  Tenes   $500.00 disponibles.  ")
        );
    }

    @Test
    void keepsNullShortAndNonListAnswersStable() {
        assertEquals("", formatter.format(null));
        assertEquals("uno; dos", formatter.format("uno; dos"));
        assertEquals("uno; ; dos; tres", formatter.format("uno; ; dos; tres"));
    }
}
