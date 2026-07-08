package com.recred.backend.application;

import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class SchoolAssistantIntentDetector {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "\\b(\\d{1,2})/(\\d{1,2})(?:/(\\d{2}|\\d{4}))?\\b"
    );

    public SchoolAssistantIntentDetector() {
    }

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

    Optional<LocalDate> purchaseDate(String message, LocalDate today) {
        String normalized = normalize(message);

        if (containsAny(normalized, normalize("pasado mañana"))) {
            return Optional.of(today.plusDays(2));
        }

        if (containsAny(normalized, normalize("mañana"))) {
            return Optional.of(today.plusDays(1));
        }

        if (containsAny(normalized, "hoy")) {
            return Optional.of(today);
        }

        Optional<LocalDate> explicitDate = explicitDate(normalized, today);

        if (explicitDate.isPresent()) {
            return explicitDate;
        }

        return dayOfWeek(normalized).map(day -> nextOrSame(today, day));
    }

    private SchoolAssistantIntent detectNonConfirmationIntent(String normalized) {
        return isPurchaseCancellation(normalized)
                ? SchoolAssistantIntent.CANCEL_PURCHASE
                : detectDraftCancellationOrRecessIntent(normalized);
    }

    private SchoolAssistantIntent detectDraftCancellationOrRecessIntent(String normalized) {
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
        return isPurchaseDateSelection(normalized)
                ? SchoolAssistantIntent.SELECT_PURCHASE_DATE
                : detectFrequentPurchaseOrOperationalIntent(normalized);
    }

    private SchoolAssistantIntent detectFrequentPurchaseOrOperationalIntent(String normalized) {
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

    private boolean isPurchaseCancellation(String message) {
        return containsAny(message, "cancelar", "cancela", "anular", "anula")
                && containsAny(message, "compra", "pedido", "orden");
    }

    private boolean isPurchaseDateSelection(String message) {
        return isRelativeDateSelection(message)
                || isWeekdaySelection(message)
                || isExplicitDateSelection(message);
    }

    private boolean isRelativeDateSelection(String message) {
        return List.of("hoy", normalize("mañana"), "para hoy", normalize("para mañana"),
                        normalize("pasado mañana"), normalize("para pasado mañana"))
                .contains(message);
    }

    private boolean isWeekdaySelection(String message) {
        return dayOfWeekName(withoutDateSelectionPrefix(message)).isPresent();
    }

    private boolean isExplicitDateSelection(String message) {
        return DATE_PATTERN.matcher(withoutDateSelectionPrefix(message)).matches();
    }

    private String withoutDateSelectionPrefix(String message) {
        return message
                .replaceFirst("^para el ", "")
                .replaceFirst("^para ", "")
                .replaceFirst("^el ", "");
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

    private Optional<LocalDate> explicitDate(String message, LocalDate today) {
        java.util.regex.Matcher matcher = DATE_PATTERN.matcher(message);

        if (!matcher.find()) {
            return Optional.empty();
        }

        int day = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        int year = explicitYear(matcher.group(3), today.getYear());

        try {
            LocalDate date = LocalDate.of(year, month, day);
            return Optional.of(date.isBefore(today) && matcher.group(3) == null
                    ? date.plusYears(1)
                    : date);
        } catch (java.time.DateTimeException exception) {
            return Optional.empty();
        }
    }

    private int explicitYear(String value, int currentYear) {
        if (value == null) {
            return currentYear;
        }

        int year = Integer.parseInt(value);
        return value.length() == 2 ? 2000 + year : year;
    }

    private Optional<DayOfWeek> dayOfWeek(String message) {
        return Arrays.stream(message.split("\\s+"))
                .map(this::dayOfWeekName)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<DayOfWeek> dayOfWeekName(String value) {
        return switch (value) {
            case "lunes" -> Optional.of(DayOfWeek.MONDAY);
            case "martes" -> Optional.of(DayOfWeek.TUESDAY);
            case "miercoles" -> Optional.of(DayOfWeek.WEDNESDAY);
            case "jueves" -> Optional.of(DayOfWeek.THURSDAY);
            case "viernes" -> Optional.of(DayOfWeek.FRIDAY);
            case "sabado" -> Optional.of(DayOfWeek.SATURDAY);
            case "domingo" -> Optional.of(DayOfWeek.SUNDAY);
            default -> Optional.empty();
        };
    }

    private LocalDate nextOrSame(LocalDate today, DayOfWeek day) {
        int daysUntil = day.getValue() - today.getDayOfWeek().getValue();
        return today.plusDays(daysUntil < 0 ? daysUntil + 7 : daysUntil);
    }

    private String normalize(String message) {
        String lowerCaseMessage = message.toLowerCase(Locale.ROOT).trim();
        String normalizedMessage = Normalizer.normalize(lowerCaseMessage, Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalizedMessage).replaceAll("");
    }
}
