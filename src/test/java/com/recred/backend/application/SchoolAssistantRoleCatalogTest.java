package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantCapability;
import com.recred.backend.domain.models.SchoolAssistantRole;
import org.junit.jupiter.api.Test;

import static com.recred.backend.domain.models.SchoolAssistantActionType.COMPRA_ANTICIPADA;
import static com.recred.backend.domain.models.SchoolAssistantActionType.CONSULTA_PEDIDOS;
import static com.recred.backend.domain.models.SchoolAssistantActionType.CONSULTA_RESTRICCIONES;
import static com.recred.backend.domain.models.SchoolAssistantCapability.PRESUPUESTOS;
import static com.recred.backend.domain.models.SchoolAssistantCapability.SALDO;
import static com.recred.backend.domain.models.SchoolAssistantCapability.STOCK;
import static com.recred.backend.domain.models.SchoolAssistantRole.ALUMNO;
import static com.recred.backend.domain.models.SchoolAssistantRole.KIOSQUERO;
import static com.recred.backend.domain.models.SchoolAssistantRole.TUTOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchoolAssistantRoleCatalogTest {

    private final SchoolAssistantRoleCatalog catalog = new SchoolAssistantRoleCatalog();

    @Test
    void shouldKeepStudentCapabilitiesFallbackAndSuggestions() {
        thenRoleHasCapability(ALUMNO, SALDO, 0);
        thenFallbackContains(ALUMNO, "saldos, pagos, compras");
        thenRoleHasSuggestionAction(ALUMNO, COMPRA_ANTICIPADA);
    }

    @Test
    void shouldKeepTutorCapabilitiesAndSuggestions() {
        thenRoleHasCapability(TUTOR, PRESUPUESTOS);
        thenRoleHasSuggestionAction(TUTOR, CONSULTA_RESTRICCIONES);
    }

    @Test
    void shouldKeepKiosqueroCapabilitiesAndSuggestions() {
        thenRoleHasCapability(KIOSQUERO, STOCK);
        thenRoleHasSuggestionAction(KIOSQUERO, CONSULTA_PEDIDOS);
    }

    private void thenRoleHasCapability(
            SchoolAssistantRole role,
            SchoolAssistantCapability expectedCapability,
            int index) {
        assertEquals(expectedCapability, catalog.capabilitiesFor(role).get(index));
    }

    private void thenRoleHasCapability(
            SchoolAssistantRole role,
            SchoolAssistantCapability expectedCapability) {
        assertTrue(catalog.capabilitiesFor(role).contains(expectedCapability));
    }

    private void thenFallbackContains(SchoolAssistantRole role, String expectedText) {
        assertTrue(catalog.fallbackAnswer(role).contains(expectedText));
    }

    private void thenRoleHasSuggestionAction(
            SchoolAssistantRole role,
            SchoolAssistantActionType expectedAction) {
        assertTrue(catalog.defaultSuggestions(role).stream()
                .anyMatch(suggestion -> suggestion.actionType() == expectedAction));
    }
}

