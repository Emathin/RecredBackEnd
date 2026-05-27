package com.recred.backend.domain.services;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.DateRange;
import com.recred.backend.domain.models.SchoolAssistantCapability;
import com.recred.backend.domain.models.SchoolAssistantQuery;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SchoolAssistantLocalQueryInterpreter {

    private static final int DEFAULT_PAYMENT_LIMIT = 5;
    private static final int SINGLE_PAYMENT_LIMIT = 1;
    private static final int MAX_PAYMENT_LIMIT = 10;
    private static final int UPCOMING_EVENT_DAYS = 7;
    private static final Pattern RECENT_LIMIT_PATTERN = Pattern.compile(
            "(ultimos|ultimas)\\s+(\\d+)"
    );
    private static final List<String> BALANCE_KEYWORDS = List.of(
            "saldo",
            "balance",
            "credito",
            "creditos",
            "plata",
            "disponible"
    );
    private static final List<String> PAYMENT_KEYWORDS = List.of(
            "pago",
            "pagos",
            "recarga",
            "recargas",
            "mercado pago",
            "acreditacion",
            "cargue",
            "transferencia"
    );
    private static final List<String> EVENT_KEYWORDS = List.of(
            "evento",
            "eventos",
            "calendario",
            "feriado",
            "acto",
            "clase",
            "clases",
            "colegio",
            "escuela"
    );
    private static final List<String> FOLLOW_UP_REFERENCES = List.of(
            "entonces",
            "anterior",
            "anteriores",
            "siguiente",
            "siguientes",
            "eso",
            "estos",
            "esas",
            "y "
    );

    public Optional<SchoolAssistantQuery> interpret(
            String question,
            LocalDate currentDate,
            List<ChatMessage> recentMessages
    ) {
        String normalizedQuestion = normalize(question);
        List<SchoolAssistantCapability> capabilities = capabilities(normalizedQuestion);

        if (requiresAiInterpretation(normalizedQuestion, capabilities, recentMessages)) {
            return Optional.empty();
        }

        if (capabilities.isEmpty()) {
            return obviousOutOfScope(normalizedQuestion)
                    ? Optional.of(outOfScopeQuery())
                    : Optional.empty();
        }

        return Optional.of(query(capabilities, normalizedQuestion, currentDate));
    }

    public SchoolAssistantQuery fallback(String question, LocalDate currentDate) {
        String normalizedQuestion = normalize(question);
        List<SchoolAssistantCapability> capabilities = capabilities(normalizedQuestion);

        if (capabilities.isEmpty()) {
            return outOfScopeQuery();
        }

        return query(capabilities, normalizedQuestion, currentDate);
    }

    private SchoolAssistantQuery query(
            List<SchoolAssistantCapability> capabilities,
            String question,
            LocalDate currentDate
    ) {
        return new SchoolAssistantQuery(
                capabilities,
                paymentDateRange(capabilities, question, currentDate),
                eventDateRange(capabilities, question, currentDate),
                paymentLimit(question),
                requiresAiResponse(capabilities)
        );
    }

    private Optional<DateRange> paymentDateRange(
            List<SchoolAssistantCapability> capabilities,
            String question,
            LocalDate currentDate
    ) {
        return hasCapability(capabilities, SchoolAssistantCapability.PAGOS)
                ? dateRange(question, currentDate)
                : Optional.empty();
    }

    private Optional<DateRange> eventDateRange(
            List<SchoolAssistantCapability> capabilities,
            String question,
            LocalDate currentDate
    ) {
        if (!hasCapability(capabilities, SchoolAssistantCapability.EVENTOS)) {
            return Optional.empty();
        }

        return dateRange(question, currentDate)
                .or(() -> Optional.of(defaultUpcomingEventRange(currentDate)));
    }

    private Optional<DateRange> dateRange(String question, LocalDate currentDate) {
        if (question.contains("manana")) {
            LocalDate tomorrow = currentDate.plusDays(1);
            return Optional.of(new DateRange(tomorrow, tomorrow));
        }

        if (question.contains("hoy")) {
            return Optional.of(new DateRange(currentDate, currentDate));
        }

        if (question.contains("semana que viene") || question.contains("proxima semana")) {
            LocalDate startDate = currentDate.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
            return Optional.of(new DateRange(startDate, startDate.plusDays(6)));
        }

        if (question.contains("esta semana")) {
            LocalDate startDate = currentDate.with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
            );
            return Optional.of(new DateRange(startDate, startDate.plusDays(6)));
        }

        if (question.contains("este mes")) {
            return Optional.of(monthRange(currentDate.getMonth(), currentDate.getYear()));
        }

        return month(question, currentDate.getYear()).map(month -> monthRange(
                month,
                currentDate.getYear()
        ));
    }

    private Optional<Month> month(String question, int currentYear) {
        return Stream.of(Month.values())
                .filter(month -> question.contains(monthName(month)))
                .findFirst();
    }

    private String monthName(Month month) {
        return switch (month) {
            case JANUARY -> "enero";
            case FEBRUARY -> "febrero";
            case MARCH -> "marzo";
            case APRIL -> "abril";
            case MAY -> "mayo";
            case JUNE -> "junio";
            case JULY -> "julio";
            case AUGUST -> "agosto";
            case SEPTEMBER -> "septiembre";
            case OCTOBER -> "octubre";
            case NOVEMBER -> "noviembre";
            case DECEMBER -> "diciembre";
        };
    }

    private DateRange monthRange(Month month, int year) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        return new DateRange(startDate, startDate.with(TemporalAdjusters.lastDayOfMonth()));
    }

    private DateRange defaultUpcomingEventRange(LocalDate currentDate) {
        return new DateRange(currentDate, currentDate.plusDays(UPCOMING_EVENT_DAYS));
    }

    private int paymentLimit(String question) {
        Matcher matcher = RECENT_LIMIT_PATTERN.matcher(question);

        if (matcher.find()) {
            return Math.min(MAX_PAYMENT_LIMIT, Integer.parseInt(matcher.group(2)));
        }

        if (question.contains("ultimo") || question.contains("ultima")) {
            return SINGLE_PAYMENT_LIMIT;
        }

        return DEFAULT_PAYMENT_LIMIT;
    }

    private List<SchoolAssistantCapability> capabilities(String question) {
        return List.of(
                        capability(
                                question,
                                BALANCE_KEYWORDS,
                                SchoolAssistantCapability.SALDO
                        ),
                        capability(
                                question,
                                PAYMENT_KEYWORDS,
                                SchoolAssistantCapability.PAGOS
                        ),
                        capability(
                                question,
                                EVENT_KEYWORDS,
                                SchoolAssistantCapability.EVENTOS
                        )
                ).stream()
                .flatMap(OptionalCapability::stream)
                .distinct()
                .toList();
    }

    private OptionalCapability capability(
            String question,
            List<String> keywords,
            SchoolAssistantCapability capability
    ) {
        return containsAny(question, keywords)
                ? OptionalCapability.of(capability)
                : OptionalCapability.empty();
    }

    private boolean requiresAiInterpretation(
            String question,
            List<SchoolAssistantCapability> capabilities,
            List<ChatMessage> recentMessages
    ) {
        return hasRecentMessages(recentMessages)
                && hasFollowUpReference(question)
                && capabilities.isEmpty();
    }

    private boolean hasRecentMessages(List<ChatMessage> recentMessages) {
        return recentMessages != null && !recentMessages.isEmpty();
    }

    private boolean hasFollowUpReference(String question) {
        return FOLLOW_UP_REFERENCES.stream().anyMatch(question::startsWith)
                || FOLLOW_UP_REFERENCES.stream().anyMatch(question::contains);
    }

    private boolean obviousOutOfScope(String question) {
        return question.equals("hola")
                || question.equals("buenas")
                || question.equals("gracias")
                || question.contains("chiste");
    }

    private boolean requiresAiResponse(List<SchoolAssistantCapability> capabilities) {
        return capabilities.size() > 1;
    }

    private boolean hasCapability(
            List<SchoolAssistantCapability> capabilities,
            SchoolAssistantCapability capability
    ) {
        return capabilities.contains(capability);
    }

    private boolean containsAny(String question, List<String> keywords) {
        return keywords.stream().anyMatch(question::contains);
    }

    private String normalize(String question) {
        String text = question == null ? "" : question;
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT);
    }

    private SchoolAssistantQuery outOfScopeQuery() {
        return new SchoolAssistantQuery(
                List.of(SchoolAssistantCapability.FUERA_DE_ALCANCE),
                Optional.empty(),
                Optional.empty(),
                DEFAULT_PAYMENT_LIMIT,
                false
        );
    }

    private record OptionalCapability(SchoolAssistantCapability value) {

        static OptionalCapability of(SchoolAssistantCapability value) {
            return new OptionalCapability(value);
        }

        static OptionalCapability empty() {
            return new OptionalCapability(null);
        }

        Stream<SchoolAssistantCapability> stream() {
            return value == null ? Stream.empty() : Stream.of(value);
        }
    }
}
