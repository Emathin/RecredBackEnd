package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolAssistantAction;
import com.recred.backend.domain.models.SchoolAssistantActionStatus;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.SchoolAssistantSuggestion;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchoolAssistantSuggestionFactoryTest {

    private final SchoolAssistantSuggestionFactory factory =
            new SchoolAssistantSuggestionFactory();

    @Test
    void suggestionsDefaultsNullRoleToStudentSuggestions() {
        List<SchoolAssistantSuggestion> suggestions =
                factory.suggestions(null, SchoolAssistantIntent.BALANCE, null);

        assertEquals(2, suggestions.size());
        assertTrue(suggestions.stream().anyMatch(
                suggestion -> suggestion.actionType()
                        == SchoolAssistantActionType.CONSULTA_COMPRAS
        ));
        assertTrue(suggestions.stream().anyMatch(
                suggestion -> suggestion.actionType()
                        == SchoolAssistantActionType.COMPRA_ANTICIPADA
        ));
    }

    @Test
    void tutorAndKiosqueroUnknownIntentReturnRoleDefaults() {
        List<SchoolAssistantSuggestion> tutorSuggestions = factory.suggestions(
                SchoolAssistantRole.TUTOR,
                SchoolAssistantIntent.UNKNOWN,
                null
        );
        List<SchoolAssistantSuggestion> kiosqueroSuggestions = factory.suggestions(
                SchoolAssistantRole.KIOSQUERO,
                SchoolAssistantIntent.UNKNOWN,
                null
        );

        assertEquals(4, tutorSuggestions.size());
        assertTrue(tutorSuggestions.stream().anyMatch(
                suggestion -> suggestion.actionType()
                        == SchoolAssistantActionType.CONSULTA_PRESUPUESTOS
        ));
        assertEquals(3, kiosqueroSuggestions.size());
        assertTrue(kiosqueroSuggestions.stream().anyMatch(
                suggestion -> suggestion.actionType() == SchoolAssistantActionType.CONSULTA_STOCK
        ));
    }

    @Test
    void advancePurchaseActionForStudentReturnsStatusSpecificSuggestions() {
        List<SchoolAssistantSuggestion> recessSuggestions = factory.suggestions(
                SchoolAssistantRole.ALUMNO,
                SchoolAssistantIntent.BUY_USUAL,
                action(SchoolAssistantActionStatus.ESPERANDO_RECREO)
        );
        List<SchoolAssistantSuggestion> confirmationSuggestions = factory.suggestions(
                SchoolAssistantRole.ALUMNO,
                SchoolAssistantIntent.BUY_USUAL,
                action(SchoolAssistantActionStatus.ESPERANDO_CONFIRMACION)
        );
        List<SchoolAssistantSuggestion> executedSuggestions = factory.suggestions(
                SchoolAssistantRole.ALUMNO,
                SchoolAssistantIntent.BUY_USUAL,
                action(SchoolAssistantActionStatus.EJECUTADA)
        );

        assertEquals(List.of("Primer recreo", "Segundo recreo", "Mediodia", "Salida"),
                recessSuggestions.stream().map(SchoolAssistantSuggestion::label).toList());
        assertEquals(List.of("Confirmar", "Cancelar"),
                confirmationSuggestions.stream().map(SchoolAssistantSuggestion::label).toList());
        assertEquals(List.of("Compra habitual", "Repetir ultima"),
                executedSuggestions.stream().map(SchoolAssistantSuggestion::label).toList());
    }

    @Test
    void advancePurchaseActionForNonStudentReturnsRoleDefaultsWithoutPurchaseActions() {
        List<SchoolAssistantSuggestion> suggestions = factory.suggestions(
                SchoolAssistantRole.TUTOR,
                SchoolAssistantIntent.BUY_USUAL,
                action(SchoolAssistantActionStatus.ESPERANDO_CONFIRMACION)
        );

        assertEquals(4, suggestions.size());
        assertTrue(suggestions.stream().noneMatch(
                suggestion -> suggestion.actionType()
                        == SchoolAssistantActionType.COMPRA_ANTICIPADA
        ));
    }

    @Test
    void defaultStudentSuggestionsIncludeMainStudentActions() throws Exception {
        List<SchoolAssistantSuggestion> suggestions =
                defaultSuggestions(SchoolAssistantRole.ALUMNO);

        assertEquals(4, suggestions.size());
        assertTrue(suggestions.stream().anyMatch(
                suggestion -> suggestion.actionType() == SchoolAssistantActionType.CONSULTA_SALDO
        ));
        assertTrue(suggestions.stream().anyMatch(
                suggestion -> suggestion.actionType() == SchoolAssistantActionType.CONSULTA_EVENTOS
        ));
    }

    private SchoolAssistantAction action(SchoolAssistantActionStatus status) {
        return new SchoolAssistantAction(
                SchoolAssistantActionType.COMPRA_ANTICIPADA,
                status,
                true,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @SuppressWarnings("unchecked")
    private List<SchoolAssistantSuggestion> defaultSuggestions(
            SchoolAssistantRole role) throws Exception {
        Method method = SchoolAssistantSuggestionFactory.class
                .getDeclaredMethod("defaultSuggestions", SchoolAssistantRole.class);
        method.setAccessible(true);
        return (List<SchoolAssistantSuggestion>) method.invoke(factory, role);
    }
}
