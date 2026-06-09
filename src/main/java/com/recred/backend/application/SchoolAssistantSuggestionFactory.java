package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolAssistantAction;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.SchoolAssistantSuggestion;
import java.util.List;

class SchoolAssistantSuggestionFactory {

    List<SchoolAssistantSuggestion> suggestions(
            SchoolAssistantRole role,
            SchoolAssistantIntent intent,
            SchoolAssistantAction action
    ) {
        SchoolAssistantRole safeRole = role == null ? SchoolAssistantRole.ALUMNO : role;
        List<SchoolAssistantSuggestion> rawSuggestions = rawSuggestions(
                safeRole,
                intent,
                action
        );

        return rawSuggestions.stream()
                .filter(suggestion -> isSuggestionAllowed(safeRole, suggestion))
                .toList();
    }

    List<SchoolAssistantSuggestion> recessSuggestions() {
        return List.of(
                suggestion("Primer recreo", "primer recreo",
                        SchoolAssistantActionType.COMPRA_ANTICIPADA),
                suggestion("Segundo recreo", "segundo recreo",
                        SchoolAssistantActionType.COMPRA_ANTICIPADA),
                suggestion("Mediodia", "mediodia",
                        SchoolAssistantActionType.COMPRA_ANTICIPADA),
                suggestion("Salida", "salida",
                        SchoolAssistantActionType.COMPRA_ANTICIPADA)
        );
    }

    List<SchoolAssistantSuggestion> confirmationSuggestions() {
        return List.of(
                suggestion("Confirmar", "confirmar",
                        SchoolAssistantActionType.COMPRA_ANTICIPADA),
                suggestion("Cancelar", "cancelar",
                        SchoolAssistantActionType.COMPRA_ANTICIPADA)
        );
    }

    List<SchoolAssistantSuggestion> quickPurchaseSuggestions() {
        return List.of(
                suggestion("Compra habitual", "comprame lo de siempre",
                        SchoolAssistantActionType.COMPRA_ANTICIPADA),
                suggestion("Repetir ultima", "repeti mi ultima compra",
                        SchoolAssistantActionType.COMPRA_ANTICIPADA)
        );
    }

    private List<SchoolAssistantSuggestion> rawSuggestions(
            SchoolAssistantRole role,
            SchoolAssistantIntent intent,
            SchoolAssistantAction action
    ) {
        return isAdvancePurchaseAction(action)
                ? purchaseSuggestions(role, action)
                : switch (role) {
            case ALUMNO -> studentSuggestions(intent);
            case TUTOR -> tutorSuggestions(intent);
            case KIOSQUERO -> kiosqueroSuggestions(intent);
        };
    }

    private List<SchoolAssistantSuggestion> studentSuggestions(SchoolAssistantIntent intent) {
        return switch (intent) {
            case BALANCE -> List.of(
                    suggestion("Ultimas compras", "mostrame mis ultimas compras",
                            SchoolAssistantActionType.CONSULTA_COMPRAS),
                    suggestion("Compra habitual", "comprame lo de siempre",
                            SchoolAssistantActionType.COMPRA_ANTICIPADA)
            );
            case RECENT_PURCHASES, FREQUENT_PURCHASES -> quickPurchaseSuggestions();
            case EVENTS -> List.of(
                    suggestion("Saldo", "cuanto saldo tengo",
                            SchoolAssistantActionType.CONSULTA_SALDO),
                    suggestion("Compras", "mostrame mis ultimas compras",
                            SchoolAssistantActionType.CONSULTA_COMPRAS)
            );
            case PENDING_ORDERS, ORDER_STATUS, WITHDRAWAL_CODE -> List.of(
                    suggestion("Ultimas compras", "mostrame mis ultimas compras",
                            SchoolAssistantActionType.CONSULTA_COMPRAS),
                    suggestion("Compra habitual", "comprame lo de siempre",
                            SchoolAssistantActionType.COMPRA_ANTICIPADA)
            );
            default -> List.of();
        };
    }

    private List<SchoolAssistantSuggestion> purchaseSuggestions(
            SchoolAssistantRole role,
            SchoolAssistantAction action
    ) {
        return role == SchoolAssistantRole.ALUMNO
                ? switch (action.status()) {
            case ESPERANDO_RECREO -> recessSuggestions();
            case ESPERANDO_CONFIRMACION -> confirmationSuggestions();
            default -> quickPurchaseSuggestions();
        }
                : defaultSuggestions(role);
    }

    private boolean isAdvancePurchaseAction(SchoolAssistantAction action) {
        return action != null && action.type() == SchoolAssistantActionType.COMPRA_ANTICIPADA;
    }

    private List<SchoolAssistantSuggestion> tutorSuggestions(SchoolAssistantIntent intent) {
        return switch (intent) {
            case EVENTS -> List.of(
                    suggestion("Mis hijos", "mostrame mis hijos",
                            SchoolAssistantActionType.CONSULTA_HIJOS),
                    suggestion("Presupuestos", "ver presupuestos de mis hijos",
                            SchoolAssistantActionType.CONSULTA_PRESUPUESTOS),
                    suggestion("Restricciones", "ver restricciones de mis hijos",
                            SchoolAssistantActionType.CONSULTA_RESTRICCIONES)
            );
            case CHILDREN -> List.of(
                    suggestion("Presupuestos", "ver presupuestos de mis hijos",
                            SchoolAssistantActionType.CONSULTA_PRESUPUESTOS),
                    suggestion("Restricciones", "ver restricciones de mis hijos",
                            SchoolAssistantActionType.CONSULTA_RESTRICCIONES),
                    suggestion("Eventos", "que eventos escolares tienen mis hijos",
                            SchoolAssistantActionType.CONSULTA_EVENTOS)
            );
            case BUDGETS, RESTRICTIONS -> List.of(
                    suggestion("Mis hijos", "mostrame mis hijos",
                            SchoolAssistantActionType.CONSULTA_HIJOS),
                    suggestion("Eventos", "que eventos escolares tienen mis hijos",
                            SchoolAssistantActionType.CONSULTA_EVENTOS)
            );
            default -> defaultSuggestions(SchoolAssistantRole.TUTOR);
        };
    }

    private List<SchoolAssistantSuggestion> kiosqueroSuggestions(SchoolAssistantIntent intent) {
        return switch (intent) {
            case STOCK -> List.of(
                    suggestion("Pedidos", "ver pedidos pendientes",
                            SchoolAssistantActionType.CONSULTA_PEDIDOS),
                    suggestion("Ventas", "ventas de hoy",
                            SchoolAssistantActionType.CONSULTA_VENTAS)
            );
            case PENDING_ORDERS, ORDER_STATUS, WITHDRAWAL_CODE -> List.of(
                    suggestion("Stock", "ver stock",
                            SchoolAssistantActionType.CONSULTA_STOCK),
                    suggestion("Ventas", "ventas de hoy",
                            SchoolAssistantActionType.CONSULTA_VENTAS)
            );
            default -> defaultSuggestions(SchoolAssistantRole.KIOSQUERO);
        };
    }

    private List<SchoolAssistantSuggestion> defaultSuggestions(SchoolAssistantRole role) {
        return switch (role) {
            case ALUMNO -> List.of(
                    suggestion("Saldo", "cuanto saldo tengo",
                            SchoolAssistantActionType.CONSULTA_SALDO),
                    suggestion("Ultimas compras", "mostrame mis ultimas compras",
                            SchoolAssistantActionType.CONSULTA_COMPRAS),
                    suggestion("Compra habitual", "comprame lo de siempre",
                            SchoolAssistantActionType.COMPRA_ANTICIPADA),
                    suggestion("Eventos", "que eventos escolares tengo",
                            SchoolAssistantActionType.CONSULTA_EVENTOS)
            );
            case TUTOR -> List.of(
                    suggestion("Mis hijos", "mostrame mis hijos",
                            SchoolAssistantActionType.CONSULTA_HIJOS),
                    suggestion("Presupuestos", "ver presupuestos de mis hijos",
                            SchoolAssistantActionType.CONSULTA_PRESUPUESTOS),
                    suggestion("Restricciones", "ver restricciones de mis hijos",
                            SchoolAssistantActionType.CONSULTA_RESTRICCIONES),
                    suggestion("Eventos", "que eventos escolares tienen mis hijos",
                            SchoolAssistantActionType.CONSULTA_EVENTOS)
            );
            case KIOSQUERO -> List.of(
                    suggestion("Pedidos", "ver pedidos pendientes",
                            SchoolAssistantActionType.CONSULTA_PEDIDOS),
                    suggestion("Stock", "ver stock",
                            SchoolAssistantActionType.CONSULTA_STOCK),
                    suggestion("Ventas", "ventas de hoy",
                            SchoolAssistantActionType.CONSULTA_VENTAS)
            );
        };
    }

    private boolean isSuggestionAllowed(
            SchoolAssistantRole role,
            SchoolAssistantSuggestion suggestion
    ) {
        return switch (role) {
            case ALUMNO -> isStudentSuggestionAllowed(suggestion);
            case TUTOR -> isTutorSuggestionAllowed(suggestion);
            case KIOSQUERO -> isKiosqueroSuggestionAllowed(suggestion);
        };
    }

    private boolean isStudentSuggestionAllowed(SchoolAssistantSuggestion suggestion) {
        return List.of(
                SchoolAssistantActionType.CONSULTA_SALDO,
                SchoolAssistantActionType.CONSULTA_COMPRAS,
                SchoolAssistantActionType.CONSULTA_COMPRAS_FRECUENTES,
                SchoolAssistantActionType.COMPRA_ANTICIPADA,
                SchoolAssistantActionType.CONSULTA_EVENTOS,
                SchoolAssistantActionType.CONSULTA_MENU,
                SchoolAssistantActionType.CONSULTA_PEDIDOS,
                SchoolAssistantActionType.CONSULTA_CODIGO_RETIRO
        ).contains(suggestion.actionType());
    }

    private boolean isTutorSuggestionAllowed(SchoolAssistantSuggestion suggestion) {
        return List.of(
                SchoolAssistantActionType.CONSULTA_HIJOS,
                SchoolAssistantActionType.CONSULTA_PRESUPUESTOS,
                SchoolAssistantActionType.CONSULTA_RESTRICCIONES,
                SchoolAssistantActionType.CONSULTA_EVENTOS,
                SchoolAssistantActionType.CONSULTA_SALDO,
                SchoolAssistantActionType.CONSULTA_COMPRAS
        ).contains(suggestion.actionType());
    }

    private boolean isKiosqueroSuggestionAllowed(SchoolAssistantSuggestion suggestion) {
        return List.of(
                SchoolAssistantActionType.CONSULTA_PEDIDOS,
                SchoolAssistantActionType.CONSULTA_STOCK,
                SchoolAssistantActionType.CONSULTA_VENTAS,
                SchoolAssistantActionType.CONSULTA_COMPRAS
        ).contains(suggestion.actionType());
    }

    private SchoolAssistantSuggestion suggestion(
            String label,
            String message,
            SchoolAssistantActionType actionType
    ) {
        return new SchoolAssistantSuggestion(label, message, actionType);
    }
}
