package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolAssistantIntent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SchoolAssistantTutorFactsFormatter {

    public SchoolAssistantTutorFactsFormatter() {
    }

    List<String> children(List<String> children) {
        return tutorChildren(children).stream()
                .map(this::childSummary)
                .toList();
    }

    List<String> facts(SchoolAssistantIntent intent, List<String> children) {
        return switch (intent) {
            case BUDGETS -> tutorChildren(children).stream()
                    .map(this::budgetSummary)
                    .toList();
            case RESTRICTIONS -> tutorChildren(children).stream()
                    .map(this::restrictionSummary)
                    .toList();
            default -> children(children);
        };
    }

    private List<TutorChildFact> tutorChildren(List<String> children) {
        return children.stream()
                .map(this::tutorChild)
                .toList();
    }

    private TutorChildFact tutorChild(String fact) {
        List<String> parts = Arrays.stream(fact.split("\\|"))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
        Map<String, String> values = new LinkedHashMap<>();
        parts.stream()
                .skip(1)
                .map(this::factEntry)
                .flatMap(Optional::stream)
                .forEach(entry -> values.put(entry.key(), entry.value()));
        String name = parts.isEmpty() ? "Alumno" : parts.get(0);
        return new TutorChildFact(name, values);
    }

    private Optional<FactEntry> factEntry(String part) {
        int separator = part.indexOf('=');
        return separator <= 0
                ? Optional.empty()
                : Optional.of(new FactEntry(
                        part.substring(0, separator),
                        part.substring(separator + 1)
                ));
    }

    private String childSummary(TutorChildFact child) {
        return child.name()
                + gradeText(child.value("grado"))
                + ". Saldo: "
                + amountText(child.value("saldo"))
                + ". Presupuesto: "
                + shortBudgetText(child.value("presupuesto"))
                + ".";
    }

    private String budgetSummary(TutorChildFact child) {
        return child.name()
                + ": "
                + budgetText(child.value("presupuesto"))
                + ". Saldo: "
                + amountText(child.value("saldo"))
                + ".";
    }

    private String restrictionSummary(TutorChildFact child) {
        List<String> healthRestrictions = listText(child.value("restriccionesSalud"));
        List<String> blockedProducts = listText(child.value("productosBloqueados"));

        return healthRestrictions.isEmpty() && blockedProducts.isEmpty()
                ? child.name() + ": sin restricciones registradas."
                : child.name() + ": " + restrictionDetails(
                        healthRestrictions,
                        blockedProducts
                ) + ".";
    }

    private String restrictionDetails(
            List<String> healthRestrictions,
            List<String> blockedProducts
    ) {
        return List.of(
                        restrictionsDetail("Salud", healthRestrictions),
                        restrictionsDetail("Productos bloqueados", blockedProducts)
                )
                .stream()
                .filter(detail -> !detail.isBlank())
                .collect(Collectors.joining(". "));
    }

    private String restrictionsDetail(String label, List<String> values) {
        return values.isEmpty() ? "" : label + ": " + String.join(", ", values);
    }

    private String gradeText(String grade) {
        return hasValue(grade) ? " - " + grade : "";
    }

    private String budgetText(String budget) {
        return hasNoBudget(budget) ? "sin presupuesto activo" : activeBudgetText(budget);
    }

    private String activeBudgetText(String budget) {
        Map<String, String> values = commaSeparatedValues(budget);
        String amount = values.get("monto");
        String period = values.get("periodo");
        String date = values.get("fechaInicio");

        return hasValue(amount)
                ? "Limite" + periodText(period) + ": " + amountText(amount) + dateText(date)
                : "Limite: " + amountText(budget);
    }

    private String shortBudgetText(String budget) {
        return hasNoBudget(budget) ? "sin presupuesto activo" : budgetText(budget);
    }

    private String periodText(String period) {
        return hasValue(period) ? " " + period.toLowerCase(Locale.ROOT) : "";
    }

    private String dateText(String date) {
        return hasValue(date) ? ". Desde: " + date : "";
    }

    private boolean hasNoBudget(String budget) {
        return !hasValue(budget) || "sin_presupuesto".equals(budget);
    }

    private Map<String, String> commaSeparatedValues(String value) {
        Map<String, String> values = new LinkedHashMap<>();
        Arrays.stream(value.split(","))
                .map(String::trim)
                .map(this::factEntry)
                .flatMap(Optional::stream)
                .forEach(entry -> values.put(entry.key(), entry.value()));
        return values;
    }

    private List<String> listText(String value) {
        return !hasValue(value) || "[]".equals(value)
                ? List.of()
                : splitListText(value);
    }

    private List<String> splitListText(String value) {
        String cleanValue = value.trim();
        String withoutBrackets = cleanValue.startsWith("[") && cleanValue.endsWith("]")
                ? cleanValue.substring(1, cleanValue.length() - 1)
                : cleanValue;
        return Arrays.stream(withoutBrackets.split(","))
                .map(String::trim)
                .filter(this::hasValue)
                .toList();
    }

    private String amountText(String amount) {
        try {
            return money(new BigDecimal(amount));
        } catch (NumberFormatException exception) {
            return hasValue(amount) ? amount : "sin datos";
        }
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank() && !"sin_datos".equals(value);
    }

    private String money(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        return "$" + safeAmount.setScale(2, RoundingMode.HALF_UP);
    }

    private record FactEntry(
            String key,
            String value
    ) {
    }

    private record TutorChildFact(
            String name,
            Map<String, String> values
    ) {
        private String value(String key) {
            return values.getOrDefault(key, "");
        }
    }
}
