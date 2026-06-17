package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantCapability;
import com.recred.backend.domain.models.SchoolAssistantRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchoolAssistantRoleCatalogTest {

    private final SchoolAssistantRoleCatalog catalog = new SchoolAssistantRoleCatalog();

    @Test
    void studentRoleKeepsCapabilitiesFallbackAndSuggestions() {
        assertEquals(
                SchoolAssistantCapability.SALDO,
                catalog.capabilitiesFor(SchoolAssistantRole.ALUMNO).get(0)
        );
        assertTrue(catalog.fallbackAnswer(SchoolAssistantRole.ALUMNO)
                .contains("saldos, pagos, compras"));
        assertTrue(catalog.defaultSuggestions(SchoolAssistantRole.ALUMNO)
                .stream()
                .anyMatch(suggestion -> suggestion.actionType()
                        == SchoolAssistantActionType.COMPRA_ANTICIPADA));
    }

    @Test
    void tutorAndKiosqueroRolesKeepSpecificCapabilities() {
        assertTrue(catalog.capabilitiesFor(SchoolAssistantRole.TUTOR)
                .contains(SchoolAssistantCapability.PRESUPUESTOS));
        assertTrue(catalog.defaultSuggestions(SchoolAssistantRole.TUTOR)
                .stream()
                .anyMatch(suggestion -> suggestion.actionType()
                        == SchoolAssistantActionType.CONSULTA_RESTRICCIONES));
        assertTrue(catalog.capabilitiesFor(SchoolAssistantRole.KIOSQUERO)
                .contains(SchoolAssistantCapability.STOCK));
        assertTrue(catalog.defaultSuggestions(SchoolAssistantRole.KIOSQUERO)
                .stream()
                .anyMatch(suggestion -> suggestion.actionType()
                        == SchoolAssistantActionType.CONSULTA_PEDIDOS));
    }
}
