package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.SchoolAssistantAction;
import com.recred.backend.domain.models.SchoolAssistantActionStatus;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.SchoolAssistantSuggestion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchoolAssistantSuggestionFactoryTest {

    private final SchoolAssistantSuggestionFactory factory =
            new SchoolAssistantSuggestionFactory();

    private List<SchoolAssistantSuggestion> suggestions;
    private List<SchoolAssistantSuggestion> tutorSuggestions;
    private List<SchoolAssistantSuggestion> kiosqueroSuggestions;

    @Test
    void suggestions_WhenRoleIsNull_UsesStudentSuggestions() {
        whenRequestingSuggestions(null, SchoolAssistantIntent.BALANCE, null);

        thenSuggestionsSizeIs(2);
        thenSuggestionsInclude(SchoolAssistantActionType.CONSULTA_COMPRAS);
        thenSuggestionsInclude(SchoolAssistantActionType.COMPRA_ANTICIPADA);
    }

    @Test
    void suggestions_WhenTutorAndKiosqueroIntentIsUnknown_ReturnsRoleDefaults() {
        whenRequestingTutorAndKiosqueroDefaultSuggestions();

        thenTutorDefaultSuggestionsAreReturned();
        thenKiosqueroDefaultSuggestionsAreReturned();
    }

    @Test
    void suggestions_WhenAdvancePurchaseWaitsForDate_ReturnsNoDateSuggestions() {
        givenAdvancePurchaseAction(SchoolAssistantActionStatus.ESPERANDO_FECHA);

        whenRequestingStudentAdvancePurchaseSuggestions();

        thenSuggestionsAreEmpty();
    }

    @Test
    void suggestions_WhenAdvancePurchaseWaitsForRecess_ReturnsRecessSuggestions() {
        givenAdvancePurchaseAction(SchoolAssistantActionStatus.ESPERANDO_RECREO);

        whenRequestingStudentAdvancePurchaseSuggestions();

        thenSuggestionLabelsAre("Primer recreo", "Segundo recreo", "Mediodia", "Salida");
    }

    @Test
    void suggestions_WhenAdvancePurchaseWaitsForConfirmation_ReturnsConfirmationSuggestions() {
        givenAdvancePurchaseAction(SchoolAssistantActionStatus.ESPERANDO_CONFIRMACION);

        whenRequestingStudentAdvancePurchaseSuggestions();

        thenSuggestionLabelsAre("Confirmar", "Cancelar");
    }

    @Test
    void suggestions_WhenExecutedPurchaseIsCancelable_IncludesCancellationSuggestion() {
        givenExecutedAdvancePurchaseAction(EstadoCompra.PENDIENTE);

        whenRequestingStudentAdvancePurchaseSuggestions();

        thenSuggestionLabelsAre("Cancelar pedido", "Codigo retiro", "Compra habitual");
    }

    @Test
    void suggestions_WhenExecutedPurchaseIsNotCancelable_ExcludesCancellationSuggestion() {
        givenExecutedAdvancePurchaseAction(EstadoCompra.LISTO);

        whenRequestingStudentAdvancePurchaseSuggestions();

        thenSuggestionLabelsAre("Codigo retiro", "Compra habitual");
    }

    @Test
    void suggestions_WhenAdvancePurchaseBelongsToTutor_ReturnsTutorDefaults() {
        givenAdvancePurchaseAction(SchoolAssistantActionStatus.ESPERANDO_CONFIRMACION);

        whenRequestingTutorAdvancePurchaseSuggestions();

        thenSuggestionsSizeIs(4);
        thenSuggestionsDoNotInclude(SchoolAssistantActionType.COMPRA_ANTICIPADA);
    }

    @Test
    void suggestions_WhenStudentIntentIsUnknown_ReturnsMainStudentActions() {
        whenRequestingSuggestions(
                SchoolAssistantRole.ALUMNO,
                SchoolAssistantIntent.UNKNOWN,
                null
        );

        thenSuggestionsSizeIs(4);
        thenSuggestionsInclude(SchoolAssistantActionType.CONSULTA_SALDO);
        thenSuggestionsInclude(SchoolAssistantActionType.CONSULTA_EVENTOS);
    }

    private SchoolAssistantAction action;

    private void givenAdvancePurchaseAction(SchoolAssistantActionStatus status) {
        action = action(status, null);
    }

    private void givenExecutedAdvancePurchaseAction(EstadoCompra purchaseStatus) {
        action = action(SchoolAssistantActionStatus.EJECUTADA, purchaseStatus);
    }

    private void whenRequestingStudentAdvancePurchaseSuggestions() {
        whenRequestingSuggestions(
                SchoolAssistantRole.ALUMNO,
                SchoolAssistantIntent.BUY_USUAL,
                action
        );
    }

    private void whenRequestingTutorAdvancePurchaseSuggestions() {
        whenRequestingSuggestions(
                SchoolAssistantRole.TUTOR,
                SchoolAssistantIntent.BUY_USUAL,
                action
        );
    }

    private void whenRequestingTutorAndKiosqueroDefaultSuggestions() {
        tutorSuggestions = factory.suggestions(
                SchoolAssistantRole.TUTOR,
                SchoolAssistantIntent.UNKNOWN,
                null
        );
        kiosqueroSuggestions = factory.suggestions(
                SchoolAssistantRole.KIOSQUERO,
                SchoolAssistantIntent.UNKNOWN,
                null
        );
    }

    private void whenRequestingSuggestions(
            SchoolAssistantRole role,
            SchoolAssistantIntent intent,
            SchoolAssistantAction action
    ) {
        suggestions = factory.suggestions(role, intent, action);
    }

    private void thenSuggestionsSizeIs(int expectedSize) {
        assertEquals(expectedSize, suggestions.size());
    }

    private void thenSuggestionsAreEmpty() {
        assertTrue(suggestions.isEmpty());
    }

    private void thenSuggestionsInclude(SchoolAssistantActionType actionType) {
        assertTrue(suggestions.stream().anyMatch(
                suggestion -> suggestion.actionType() == actionType
        ));
    }

    private void thenSuggestionsDoNotInclude(SchoolAssistantActionType actionType) {
        assertFalse(suggestions.stream().anyMatch(
                suggestion -> suggestion.actionType() == actionType
        ));
    }

    private void thenSuggestionLabelsAre(String... expectedLabels) {
        assertEquals(List.of(expectedLabels), suggestionLabels());
    }

    private void thenTutorDefaultSuggestionsAreReturned() {
        assertEquals(4, tutorSuggestions.size());
        assertTrue(tutorSuggestions.stream().anyMatch(
                suggestion -> suggestion.actionType()
                        == SchoolAssistantActionType.CONSULTA_PRESUPUESTOS
        ));
    }

    private void thenKiosqueroDefaultSuggestionsAreReturned() {
        assertEquals(3, kiosqueroSuggestions.size());
        assertTrue(kiosqueroSuggestions.stream().anyMatch(
                suggestion -> suggestion.actionType() == SchoolAssistantActionType.CONSULTA_STOCK
        ));
    }

    private List<String> suggestionLabels() {
        return suggestions.stream()
                .map(SchoolAssistantSuggestion::label)
                .toList();
    }

    private SchoolAssistantAction action(
            SchoolAssistantActionStatus status,
            EstadoCompra purchaseStatus
    ) {
        return new SchoolAssistantAction(
                SchoolAssistantActionType.COMPRA_ANTICIPADA,
                status,
                true,
                List.of(),
                null,
                purchaseStatus,
                null,
                null,
                null,
                null
        );
    }
}
