package com.recred.backend.application;

import java.util.List;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SupplierUnitQuantityParser {

    private static final List<Pattern> QUANTITY_PATTERNS = List.of(
            Pattern.compile(
                    "(?:pack|caja|bolsa|bulto|display|promo)?\\s*(?:x|de)\\s*(\\d+)",
                    Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile(
                    "(?:pack|caja|bolsa|bulto|display|promo)\\s+(\\d+)",
                    Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile(
                    "(\\d+)\\s*(?:unidades|unids|uds|ud|un|u)\\b",
                    Pattern.CASE_INSENSITIVE
            )
    );

    int quantityFrom(String unit) {
        if (unit == null || unit.isBlank()) {
            return 1;
        }

        String normalizedUnit = unit.toLowerCase().trim();
        return QUANTITY_PATTERNS.stream()
                .map(pattern -> quantityFrom(pattern.matcher(normalizedUnit)))
                .flatMapToInt(OptionalInt::stream)
                .findFirst()
                .orElse(1);
    }

    private OptionalInt quantityFrom(Matcher matcher) {
        if (!matcher.find()) {
            return OptionalInt.empty();
        }

        try {
            int quantity = Integer.parseInt(matcher.group(1));
            return quantity > 0 ? OptionalInt.of(quantity) : OptionalInt.empty();
        } catch (NumberFormatException exception) {
            return OptionalInt.empty();
        }
    }
}
