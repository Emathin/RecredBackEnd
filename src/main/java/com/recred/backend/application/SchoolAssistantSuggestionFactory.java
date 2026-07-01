package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.SchoolAssistantAction;
import com.recred.backend.domain.models.SchoolAssistantActionStatus;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.SchoolAssistantSuggestion;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

class SchoolAssistantSuggestionFactory {

    private final EnumMap<SchoolAssistantRole, SchoolAssistantRoleSuggestionConfig>
            suggestionsByRole;
    private final EnumMap<SchoolAssistantActionStatus, List<SchoolAssistantSuggestion>>
            advancePurchaseSuggestions;

    SchoolAssistantSuggestionFactory() {
        this.suggestionsByRole = suggestionsByRole();
        this.advancePurchaseSuggestions = advancePurchaseSuggestions();
    }

    List<SchoolAssistantSuggestion> suggestions(
            SchoolAssistantRole role,
            SchoolAssistantIntent intent,
            SchoolAssistantAction action
    ) {
        SchoolAssistantRole safeRole = role == null ? SchoolAssistantRole.ALUMNO : role;
        SchoolAssistantRoleSuggestionConfig config = suggestionsByRole.get(safeRole);
        List<SchoolAssistantSuggestion> rawSuggestions = rawSuggestions(
                safeRole,
                intent,
                action,
                config
        );

        return rawSuggestions.stream()
                .filter(suggestion -> config.allows(suggestion.actionType()))
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

    List<SchoolAssistantSuggestion> dateSuggestions() {
        return List.of();
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

    private List<SchoolAssistantSuggestion> executedPurchaseSuggestions() {
        return List.of(
                suggestion("Cancelar pedido", "cancelar mi pedido",
                        SchoolAssistantActionType.CANCELACION_COMPRA),
                suggestion("Codigo retiro", "codigo de retiro",
                        SchoolAssistantActionType.CONSULTA_CODIGO_RETIRO),
                suggestion("Compra habitual", "comprame lo de siempre",
                        SchoolAssistantActionType.COMPRA_ANTICIPADA)
        );
    }

    private List<SchoolAssistantSuggestion> executedPurchaseSuggestions(
            SchoolAssistantAction action
    ) {
        return isCancelablePurchaseAction(action)
                ? executedPurchaseSuggestions()
                : executedNonCancelablePurchaseSuggestions();
    }

    private List<SchoolAssistantSuggestion> executedNonCancelablePurchaseSuggestions() {
        return List.of(
                suggestion("Codigo retiro", "codigo de retiro",
                        SchoolAssistantActionType.CONSULTA_CODIGO_RETIRO),
                suggestion("Compra habitual", "comprame lo de siempre",
                        SchoolAssistantActionType.COMPRA_ANTICIPADA)
        );
    }

    private List<SchoolAssistantSuggestion> rawSuggestions(
            SchoolAssistantRole role,
            SchoolAssistantIntent intent,
            SchoolAssistantAction action,
            SchoolAssistantRoleSuggestionConfig config
    ) {
        return isAdvancePurchaseAction(action)
                ? purchaseSuggestions(role, action, config)
                : config.suggestionsFor(intent);
    }

    private List<SchoolAssistantSuggestion> purchaseSuggestions(
            SchoolAssistantRole role,
            SchoolAssistantAction action,
            SchoolAssistantRoleSuggestionConfig config
    ) {
        if (role != SchoolAssistantRole.ALUMNO) {
            return config.defaultSuggestions();
        }

        if (action.status() == SchoolAssistantActionStatus.EJECUTADA) {
            return executedPurchaseSuggestions(action);
        }

        return advancePurchaseSuggestions.getOrDefault(
                action.status(),
                quickPurchaseSuggestions()
        );
    }

    private boolean isAdvancePurchaseAction(SchoolAssistantAction action) {
        return action != null && action.type() == SchoolAssistantActionType.COMPRA_ANTICIPADA;
    }

    private boolean isCancelablePurchaseAction(SchoolAssistantAction action) {
        return action.purchaseStatus() == EstadoCompra.PENDIENTE;
    }

    private List<SchoolAssistantSuggestion> defaultSuggestions(SchoolAssistantRole role) {
        return suggestionsByRole.get(role).defaultSuggestions();
    }

    private EnumMap<SchoolAssistantRole, SchoolAssistantRoleSuggestionConfig> suggestionsByRole() {
        EnumMap<SchoolAssistantRole, SchoolAssistantRoleSuggestionConfig> suggestions =
                new EnumMap<>(SchoolAssistantRole.class);
        suggestions.put(SchoolAssistantRole.ALUMNO, studentConfig());
        suggestions.put(SchoolAssistantRole.TUTOR, tutorConfig());
        suggestions.put(SchoolAssistantRole.KIOSQUERO, kiosqueroConfig());
        return suggestions;
    }

    private SchoolAssistantRoleSuggestionConfig studentConfig() {
        List<SchoolAssistantSuggestion> defaults = List.of(
                suggestion("Saldo", "cuanto saldo tengo",
                        SchoolAssistantActionType.CONSULTA_SALDO),
                suggestion("Ultimas compras", "mostrame mis ultimas compras",
                        SchoolAssistantActionType.CONSULTA_COMPRAS),
                suggestion("Compra habitual", "comprame lo de siempre",
                        SchoolAssistantActionType.COMPRA_ANTICIPADA),
                suggestion("Eventos", "que eventos escolares tengo",
                        SchoolAssistantActionType.CONSULTA_EVENTOS)
        );
        EnumMap<SchoolAssistantIntent, List<SchoolAssistantSuggestion>> byIntent =
                SchoolAssistantRoleSuggestionConfig.suggestionsByIntent(Map.of(
                        SchoolAssistantIntent.BALANCE,
                        List.of(
                                suggestion("Ultimas compras", "mostrame mis ultimas compras",
                                        SchoolAssistantActionType.CONSULTA_COMPRAS),
                                suggestion("Compra habitual", "comprame lo de siempre",
                                        SchoolAssistantActionType.COMPRA_ANTICIPADA)
                        ),
                        SchoolAssistantIntent.RECENT_PURCHASES,
                        quickPurchaseSuggestions(),
                        SchoolAssistantIntent.FREQUENT_PURCHASES,
                        quickPurchaseSuggestions(),
                        SchoolAssistantIntent.EVENTS,
                        List.of(
                                suggestion("Saldo", "cuanto saldo tengo",
                                        SchoolAssistantActionType.CONSULTA_SALDO),
                                suggestion("Compras", "mostrame mis ultimas compras",
                                        SchoolAssistantActionType.CONSULTA_COMPRAS)
                        ),
                        SchoolAssistantIntent.PENDING_ORDERS,
                        recentPurchasesAndQuickPurchaseSuggestions(),
                        SchoolAssistantIntent.ORDER_STATUS,
                        recentPurchasesAndQuickPurchaseSuggestions(),
                        SchoolAssistantIntent.WITHDRAWAL_CODE,
                        recentPurchasesAndQuickPurchaseSuggestions(),
                        SchoolAssistantIntent.CANCEL_PURCHASE,
                        recentPurchasesAndQuickPurchaseSuggestions()
                ));
        return new SchoolAssistantRoleSuggestionConfig(
                defaults,
                byIntent,
                EnumSet.of(
                        SchoolAssistantActionType.CONSULTA_SALDO,
                        SchoolAssistantActionType.CONSULTA_COMPRAS,
                        SchoolAssistantActionType.CONSULTA_COMPRAS_FRECUENTES,
                        SchoolAssistantActionType.COMPRA_ANTICIPADA,
                        SchoolAssistantActionType.CANCELACION_COMPRA,
                        SchoolAssistantActionType.CONSULTA_EVENTOS,
                        SchoolAssistantActionType.CONSULTA_MENU,
                        SchoolAssistantActionType.CONSULTA_PEDIDOS,
                        SchoolAssistantActionType.CONSULTA_CODIGO_RETIRO
                )
        );
    }

    private SchoolAssistantRoleSuggestionConfig tutorConfig() {
        List<SchoolAssistantSuggestion> defaults = List.of(
                suggestion("Mis hijos", "mostrame mis hijos",
                        SchoolAssistantActionType.CONSULTA_HIJOS),
                suggestion("Presupuestos", "ver presupuestos de mis hijos",
                        SchoolAssistantActionType.CONSULTA_PRESUPUESTOS),
                suggestion("Restricciones", "ver restricciones de mis hijos",
                        SchoolAssistantActionType.CONSULTA_RESTRICCIONES),
                suggestion("Eventos", "que eventos escolares tienen mis hijos",
                        SchoolAssistantActionType.CONSULTA_EVENTOS)
        );
        EnumMap<SchoolAssistantIntent, List<SchoolAssistantSuggestion>> byIntent =
                SchoolAssistantRoleSuggestionConfig.suggestionsByIntent(Map.of(
                        SchoolAssistantIntent.EVENTS,
                        List.of(
                                suggestion("Mis hijos", "mostrame mis hijos",
                                        SchoolAssistantActionType.CONSULTA_HIJOS),
                                suggestion("Presupuestos", "ver presupuestos de mis hijos",
                                        SchoolAssistantActionType.CONSULTA_PRESUPUESTOS),
                                suggestion("Restricciones", "ver restricciones de mis hijos",
                                        SchoolAssistantActionType.CONSULTA_RESTRICCIONES)
                        ),
                        SchoolAssistantIntent.CHILDREN,
                        List.of(
                                suggestion("Presupuestos", "ver presupuestos de mis hijos",
                                        SchoolAssistantActionType.CONSULTA_PRESUPUESTOS),
                                suggestion("Restricciones", "ver restricciones de mis hijos",
                                        SchoolAssistantActionType.CONSULTA_RESTRICCIONES),
                                suggestion("Eventos",
                                        "que eventos escolares tienen mis hijos",
                                        SchoolAssistantActionType.CONSULTA_EVENTOS)
                        ),
                        SchoolAssistantIntent.BUDGETS,
                        childrenAndEventsSuggestions(),
                        SchoolAssistantIntent.RESTRICTIONS,
                        childrenAndEventsSuggestions()
                ));
        return new SchoolAssistantRoleSuggestionConfig(
                defaults,
                byIntent,
                EnumSet.of(
                        SchoolAssistantActionType.CONSULTA_HIJOS,
                        SchoolAssistantActionType.CONSULTA_PRESUPUESTOS,
                        SchoolAssistantActionType.CONSULTA_RESTRICCIONES,
                        SchoolAssistantActionType.CONSULTA_EVENTOS,
                        SchoolAssistantActionType.CONSULTA_SALDO,
                        SchoolAssistantActionType.CONSULTA_COMPRAS
                )
        );
    }

    private SchoolAssistantRoleSuggestionConfig kiosqueroConfig() {
        List<SchoolAssistantSuggestion> defaults = List.of(
                suggestion("Pedidos", "ver pedidos pendientes",
                        SchoolAssistantActionType.CONSULTA_PEDIDOS),
                suggestion("Stock", "ver stock",
                        SchoolAssistantActionType.CONSULTA_STOCK),
                suggestion("Ventas", "ventas de hoy",
                        SchoolAssistantActionType.CONSULTA_VENTAS)
        );
        EnumMap<SchoolAssistantIntent, List<SchoolAssistantSuggestion>> byIntent =
                SchoolAssistantRoleSuggestionConfig.suggestionsByIntent(Map.of(
                        SchoolAssistantIntent.STOCK,
                        List.of(
                                suggestion("Pedidos", "ver pedidos pendientes",
                                        SchoolAssistantActionType.CONSULTA_PEDIDOS),
                                suggestion("Ventas", "ventas de hoy",
                                        SchoolAssistantActionType.CONSULTA_VENTAS)
                        ),
                        SchoolAssistantIntent.PENDING_ORDERS,
                        stockAndSalesSuggestions(),
                        SchoolAssistantIntent.ORDER_STATUS,
                        stockAndSalesSuggestions(),
                        SchoolAssistantIntent.WITHDRAWAL_CODE,
                        stockAndSalesSuggestions()
                ));
        return new SchoolAssistantRoleSuggestionConfig(
                defaults,
                byIntent,
                EnumSet.of(
                        SchoolAssistantActionType.CONSULTA_PEDIDOS,
                        SchoolAssistantActionType.CONSULTA_STOCK,
                        SchoolAssistantActionType.CONSULTA_VENTAS,
                        SchoolAssistantActionType.CONSULTA_COMPRAS
                )
        );
    }

    private EnumMap<SchoolAssistantActionStatus, List<SchoolAssistantSuggestion>>
    advancePurchaseSuggestions() {
        EnumMap<SchoolAssistantActionStatus, List<SchoolAssistantSuggestion>> suggestions =
                new EnumMap<>(SchoolAssistantActionStatus.class);
        suggestions.put(SchoolAssistantActionStatus.ESPERANDO_FECHA, dateSuggestions());
        suggestions.put(SchoolAssistantActionStatus.ESPERANDO_RECREO, recessSuggestions());
        suggestions.put(
                SchoolAssistantActionStatus.ESPERANDO_CONFIRMACION,
                confirmationSuggestions()
        );
        return suggestions;
    }

    private List<SchoolAssistantSuggestion> recentPurchasesAndQuickPurchaseSuggestions() {
        return List.of(
                suggestion("Ultimas compras", "mostrame mis ultimas compras",
                        SchoolAssistantActionType.CONSULTA_COMPRAS),
                suggestion("Compra habitual", "comprame lo de siempre",
                        SchoolAssistantActionType.COMPRA_ANTICIPADA)
        );
    }

    private List<SchoolAssistantSuggestion> childrenAndEventsSuggestions() {
        return List.of(
                suggestion("Mis hijos", "mostrame mis hijos",
                        SchoolAssistantActionType.CONSULTA_HIJOS),
                suggestion("Eventos", "que eventos escolares tienen mis hijos",
                        SchoolAssistantActionType.CONSULTA_EVENTOS)
        );
    }

    private List<SchoolAssistantSuggestion> stockAndSalesSuggestions() {
        return List.of(
                suggestion("Stock", "ver stock",
                        SchoolAssistantActionType.CONSULTA_STOCK),
                suggestion("Ventas", "ventas de hoy",
                        SchoolAssistantActionType.CONSULTA_VENTAS)
        );
    }

    private SchoolAssistantSuggestion suggestion(
            String label,
            String message,
            SchoolAssistantActionType actionType
    ) {
        return new SchoolAssistantSuggestion(label, message, actionType);
    }
}
