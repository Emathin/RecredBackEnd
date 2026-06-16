package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolAssistantIntent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SchoolAssistantTutorFactsFormatterTest {

    private final SchoolAssistantTutorFactsFormatter formatter =
            new SchoolAssistantTutorFactsFormatter();

    @Test
    void children_ShouldFormatDefaultChildFacts() {
        List<String> result = formatter.children(List.of(
                "Ana | grado=1A | saldo=123.456 | presupuesto=sin_presupuesto",
                "Alumno | saldo=sin_datos | presupuesto=250",
                ""
        ));

        assertEquals(
                List.of(
                        "Ana - 1A. Saldo: $123.46. Presupuesto: sin presupuesto activo.",
                        "Alumno. Saldo: sin datos. Presupuesto: Limite: $250.00.",
                        "Alumno. Saldo: sin datos. Presupuesto: sin presupuesto activo."
                ),
                result
        );
    }

    @Test
    void facts_ForBudgets_ShouldFormatStructuredBudget() {
        List<String> result = formatter.facts(
                SchoolAssistantIntent.BUDGETS,
                List.of("Ana | saldo=10 | presupuesto=monto=500,periodo=SEMANAL,fechaInicio=2026-06-15")
        );

        assertEquals(
                List.of("Ana: Limite semanal: $500.00. Desde: 2026-06-15. Saldo: $10.00."),
                result
        );
    }

    @Test
    void facts_ForDefaultIntent_ShouldFormatChildren() {
        List<String> result = formatter.facts(
                SchoolAssistantIntent.BALANCE,
                List.of("Ana | saldo=12 | presupuesto=monto=500")
        );

        assertEquals(
                List.of("Ana. Saldo: $12.00. Presupuesto: Limite: $500.00."),
                result
        );
    }

    @Test
    void facts_ForBudgets_ShouldFormatMissingPeriodAndDate() {
        List<String> result = formatter.facts(
                SchoolAssistantIntent.BUDGETS,
                List.of("Ana | saldo=no disponible | presupuesto=monto=500")
        );

        assertEquals(
                List.of("Ana: Limite: $500.00. Saldo: no disponible."),
                result
        );
    }

    @Test
    void facts_ForRestrictions_ShouldFormatEmptyAndFilledRestrictions() {
        List<String> result = formatter.facts(
                SchoolAssistantIntent.RESTRICTIONS,
                List.of(
                        "Ana | restriccionesSalud=[] | productosBloqueados=[]",
                        "Juan | restriccionesSalud=[Sin TACC, Sin Azucar] | productosBloqueados=Gaseosa"
                )
        );

        assertEquals(
                List.of(
                        "Ana: sin restricciones registradas.",
                        "Juan: Salud: Sin TACC, Sin Azucar. Productos bloqueados: Gaseosa."
                ),
                result
        );
    }

    @Test
    void facts_ForRestrictions_ShouldSkipBlankDetails() {
        List<String> result = formatter.facts(
                SchoolAssistantIntent.RESTRICTIONS,
                List.of("Ana | restriccionesSalud=[ , ] | productosBloqueados=[Gaseosa]")
        );

        assertEquals(
                List.of("Ana: Productos bloqueados: Gaseosa."),
                result
        );
    }
}
