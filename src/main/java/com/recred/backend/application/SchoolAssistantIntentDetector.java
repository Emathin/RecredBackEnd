package com.recred.backend.application;

import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

class SchoolAssistantIntentDetector {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    SchoolAssistantIntent detectIntent(String message) {
        String normalized = normalize(message);

        return isConfirmation(normalized)
                ? SchoolAssistantIntent.CONFIRM_PURCHASE
                : detectNonConfirmationIntent(normalized);
    }

    Optional<RecessTime> recessTime(String message) {
        String normalized = normalize(message);

        if (containsAll(normalized, "primer", "recreo")
                || containsAny(normalized, "first_recess", "primero")) {
            return Optional.of(RecessTime.FIRST_RECESS);
        }

        if (containsAll(normalized, "segundo", "recreo")
                || containsAny(normalized, "second_recess")) {
            return Optional.of(RecessTime.SECOND_RECESS);
        }

        if (containsAny(normalized, "mediodia", "medio dia", "noon", "almuerzo")) {
            return Optional.of(RecessTime.NOON);
        }

        if (containsAny(normalized, "salida", "after_hours", "despues de clase")) {
            return Optional.of(RecessTime.AFTER_HOURS);
        }

        return Optional.empty();
    }

    private SchoolAssistantIntent detectNonConfirmationIntent(String normalized) {
        return isCancellation(normalized)
                ? SchoolAssistantIntent.CANCEL_ACTION
                : detectRecessOrPurchaseIntent(normalized);
    }

    private SchoolAssistantIntent detectRecessOrPurchaseIntent(String normalized) {
        return recessTime(normalized).isPresent()
                ? SchoolAssistantIntent.SELECT_RECESS
                : detectPurchaseIntent(normalized);
    }

    private SchoolAssistantIntent detectPurchaseIntent(String normalized) {
        return isQuickUsualPurchase(normalized)
                ? SchoolAssistantIntent.BUY_USUAL
                : detectRepeatOrTopicIntent(normalized);
    }

    private SchoolAssistantIntent detectRepeatOrTopicIntent(String normalized) {
        return isRepeatLastPurchase(normalized)
                ? SchoolAssistantIntent.REPEAT_LAST_PURCHASE
                : detectTopicIntent(normalized);
    }

    private SchoolAssistantIntent detectTopicIntent(String normalized) {
        return isFrequentPurchasesQuestion(normalized)
                ? SchoolAssistantIntent.FREQUENT_PURCHASES
                : detectOperationalIntent(normalized);
    }

    private SchoolAssistantIntent detectOperationalIntent(String normalized) {
        if (isWithdrawalCodeQuestion(normalized)) {
            return SchoolAssistantIntent.WITHDRAWAL_CODE;
        }

        if (isPendingOrdersQuestion(normalized)) {
            return SchoolAssistantIntent.PENDING_ORDERS;
        }

        if (isStockQuestion(normalized)) {
            return SchoolAssistantIntent.STOCK;
        }

        if (isBudgetQuestion(normalized)) {
            return SchoolAssistantIntent.BUDGETS;
        }

        if (isRestrictionsQuestion(normalized)) {
            return SchoolAssistantIntent.RESTRICTIONS;
        }

        if (isEventsQuestion(normalized)) {
            return SchoolAssistantIntent.EVENTS;
        }

        if (isChildrenQuestion(normalized)) {
            return SchoolAssistantIntent.CHILDREN;
        }

        if (isMenuQuestion(normalized)) {
            return SchoolAssistantIntent.MENU;
        }

        return detectRecentOrBalanceIntent(normalized);
    }

    private SchoolAssistantIntent detectRecentOrBalanceIntent(String normalized) {
        return isRecentPurchasesQuestion(normalized)
                ? SchoolAssistantIntent.RECENT_PURCHASES
                : detectBalanceIntent(normalized);
    }

    private SchoolAssistantIntent detectBalanceIntent(String normalized) {
        return isBalanceQuestion(normalized)
                ? SchoolAssistantIntent.BALANCE
                : SchoolAssistantIntent.UNKNOWN;
    }

    private boolean isConfirmation(String message) {
        return List.of("confirmar", "confirmo", "si", "dale", "aceptar", "comprar")
                .contains(message);
    }

    private boolean isCancellation(String message) {
        return List.of("cancelar", "cancela", "descartar", "no").contains(message);
    }

    private boolean isQuickUsualPurchase(String message) {
        return containsPurchaseWord(message)
                && containsAny(message, "habitual", "siempre", "lo de siempre");
    }

    private boolean isRepeatLastPurchase(String message) {
        return containsAny(message, "repeti", "repetir", "repetime", "volver a comprar")
                && containsAny(message, "ultima compra", "ultimo pedido");
    }

    private boolean isFrequentPurchasesQuestion(String message) {
        return containsPurchaseWord(message)
                && containsAny(message, "frecuente", "habitual", "suelo", "seguido");
    }

    private boolean isRecentPurchasesQuestion(String message) {
        return containsAny(message, "ultimas compras", "historial de compras", "que compre")
                || containsAny(message, "ultima compra", "compra reciente");
    }

    private boolean isBalanceQuestion(String message) {
        return containsAny(message, "saldo", "credito", "creditos", "disponible");
    }

    private boolean isEventsQuestion(String message) {
        return containsAny(message, "evento", "eventos", "calendario", "acto",
                "jornada", "feriado", "dia escolar");
    }

    private boolean isMenuQuestion(String message) {
        return containsAny(message, "menu", "productos disponibles", "que hay",
                "que venden", "lista de productos");
    }

    private boolean isPendingOrdersQuestion(String message) {
        return containsAny(message, "pedido pendiente", "pedidos pendientes",
                "estado de pedido", "estado de mi pedido", "orden pendiente");
    }

    private boolean isWithdrawalCodeQuestion(String message) {
        return containsAny(message, "codigo de retiro", "codigo retiro",
                "codigo para retirar", "retirar pedido");
    }

    private boolean isStockQuestion(String message) {
        return containsAny(message, "stock", "inventario", "existencias");
    }

    private boolean isChildrenQuestion(String message) {
        return containsAny(message, "mis hijos", "hijos asociados", "hijos");
    }

    private boolean isBudgetQuestion(String message) {
        return containsAny(message, "presupuesto", "presupuestos", "limite");
    }

    private boolean isRestrictionsQuestion(String message) {
        return containsAny(message, "restriccion", "restricciones", "bloqueado",
                "bloqueados", "salud");
    }

    private boolean containsPurchaseWord(String message) {
        return containsAny(message, "compra", "compras", "comprar", "compre", "compro");
    }

    private boolean containsAny(String message, String... values) {
        return List.of(values).stream().anyMatch(message::contains);
    }

    private boolean containsAll(String message, String... values) {
        return List.of(values).stream().allMatch(message::contains);
    }

    private String normalize(String message) {
        String lowerCaseMessage = message.toLowerCase(Locale.ROOT).trim();
        String normalizedMessage = Normalizer.normalize(lowerCaseMessage, Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalizedMessage).replaceAll("");
    }
}
