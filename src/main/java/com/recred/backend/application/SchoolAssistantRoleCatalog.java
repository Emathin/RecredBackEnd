package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantCapability;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.SchoolAssistantSuggestion;
import java.util.EnumMap;
import java.util.List;

public class SchoolAssistantRoleCatalog {

    private static final String STUDENT_FALLBACK =
            "No pude responder ahora. Puedo ayudarte con saldos, pagos, compras "
                    + "y eventos escolares.";
    private static final String TUTOR_FALLBACK =
            "No pude responder ahora. Puedo ayudarte con saldos, pagos, compras, "
                    + "eventos, presupuestos y restricciones de tus hijos.";
    private static final String KIOSQUERO_FALLBACK =
            "No pude responder ahora. Puedo ayudarte con stock, ventas y pedidos del buffet.";

    private final EnumMap<SchoolAssistantRole, List<SchoolAssistantCapability>> capabilities;
    private final EnumMap<SchoolAssistantRole, String> fallbackAnswers;
    private final EnumMap<SchoolAssistantRole, List<SchoolAssistantSuggestion>>
            defaultSuggestions;

    public SchoolAssistantRoleCatalog() {
        this.capabilities = capabilities();
        this.fallbackAnswers = fallbackAnswers();
        this.defaultSuggestions = defaultSuggestions();
    }

    List<SchoolAssistantCapability> capabilitiesFor(SchoolAssistantRole role) {
        return capabilities.get(role);
    }

    String fallbackAnswer(SchoolAssistantRole role) {
        return fallbackAnswers.get(role);
    }

    List<SchoolAssistantSuggestion> defaultSuggestions(SchoolAssistantRole role) {
        return defaultSuggestions.get(role);
    }

    private EnumMap<SchoolAssistantRole, List<SchoolAssistantCapability>> capabilities() {
        EnumMap<SchoolAssistantRole, List<SchoolAssistantCapability>> values =
                new EnumMap<>(SchoolAssistantRole.class);
        values.put(SchoolAssistantRole.ALUMNO, List.of(
                SchoolAssistantCapability.SALDO,
                SchoolAssistantCapability.PAGOS,
                SchoolAssistantCapability.COMPRAS,
                SchoolAssistantCapability.EVENTOS
        ));
        values.put(SchoolAssistantRole.TUTOR, List.of(
                SchoolAssistantCapability.HIJOS,
                SchoolAssistantCapability.SALDO,
                SchoolAssistantCapability.PAGOS,
                SchoolAssistantCapability.COMPRAS,
                SchoolAssistantCapability.EVENTOS,
                SchoolAssistantCapability.PRESUPUESTOS,
                SchoolAssistantCapability.RESTRICCIONES
        ));
        values.put(SchoolAssistantRole.KIOSQUERO, List.of(
                SchoolAssistantCapability.STOCK,
                SchoolAssistantCapability.VENTAS,
                SchoolAssistantCapability.PEDIDOS,
                SchoolAssistantCapability.COMPRAS
        ));
        return values;
    }

    private EnumMap<SchoolAssistantRole, String> fallbackAnswers() {
        EnumMap<SchoolAssistantRole, String> values =
                new EnumMap<>(SchoolAssistantRole.class);
        values.put(SchoolAssistantRole.ALUMNO, STUDENT_FALLBACK);
        values.put(SchoolAssistantRole.TUTOR, TUTOR_FALLBACK);
        values.put(SchoolAssistantRole.KIOSQUERO, KIOSQUERO_FALLBACK);
        return values;
    }

    private EnumMap<SchoolAssistantRole, List<SchoolAssistantSuggestion>> defaultSuggestions() {
        EnumMap<SchoolAssistantRole, List<SchoolAssistantSuggestion>> values =
                new EnumMap<>(SchoolAssistantRole.class);
        values.put(SchoolAssistantRole.ALUMNO, List.of(
                suggestion("Saldo", "cuanto saldo tengo",
                        SchoolAssistantActionType.CONSULTA_SALDO),
                suggestion("Ultimas compras", "mostrame mis ultimas compras",
                        SchoolAssistantActionType.CONSULTA_COMPRAS),
                suggestion("Compra habitual", "comprame lo de siempre",
                        SchoolAssistantActionType.COMPRA_ANTICIPADA),
                suggestion("Eventos", "que eventos escolares tengo",
                        SchoolAssistantActionType.CONSULTA_EVENTOS)
        ));
        values.put(SchoolAssistantRole.TUTOR, List.of(
                suggestion("Mis hijos", "mostrame mis hijos",
                        SchoolAssistantActionType.CONSULTA_HIJOS),
                suggestion("Presupuestos", "ver presupuestos de mis hijos",
                        SchoolAssistantActionType.CONSULTA_PRESUPUESTOS),
                suggestion("Restricciones", "ver restricciones de mis hijos",
                        SchoolAssistantActionType.CONSULTA_RESTRICCIONES),
                suggestion("Eventos", "que eventos escolares tienen mis hijos",
                        SchoolAssistantActionType.CONSULTA_EVENTOS)
        ));
        values.put(SchoolAssistantRole.KIOSQUERO, List.of(
                suggestion("Pedidos", "ver pedidos pendientes",
                        SchoolAssistantActionType.CONSULTA_PEDIDOS),
                suggestion("Stock", "ver stock",
                        SchoolAssistantActionType.CONSULTA_STOCK),
                suggestion("Ventas", "ventas de hoy",
                        SchoolAssistantActionType.CONSULTA_VENTAS)
        ));
        return values;
    }

    private SchoolAssistantSuggestion suggestion(
            String label,
            String message,
            SchoolAssistantActionType actionType
    ) {
        return new SchoolAssistantSuggestion(label, message, actionType);
    }
}
