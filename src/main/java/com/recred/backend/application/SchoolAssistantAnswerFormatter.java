package com.recred.backend.application;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SchoolAssistantAnswerFormatter {

    private static final int MIN_SEMICOLON_ITEMS = 3;
    private static final int MAX_TITLE_LENGTH = 80;
    private static final Pattern SPACES = Pattern.compile("[ \\t]+");
    private static final Pattern BLANK_LINES = Pattern.compile("\\n{3,}");

    public String format(String answer) {
        return answer == null ? "" : formatPresentAnswer(answer);
    }

    private String formatPresentAnswer(String answer) {
        String cleanAnswer = normalizeSpacing(repairMojibake(answer));
        return semicolonList(cleanAnswer)
                .filter(list -> shouldFormatAsList(cleanAnswer, list))
                .map(this::formattedList)
                .orElse(cleanAnswer);
    }

    private String normalizeSpacing(String answer) {
        String normalizedLineBreaks = answer
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        String normalizedSpaces = normalizedLineBreaks.lines()
                .map(String::trim)
                .map(line -> SPACES.matcher(line).replaceAll(" "))
                .collect(Collectors.joining("\n"))
                .trim();
        return BLANK_LINES.matcher(normalizedSpaces).replaceAll("\n\n");
    }

    private String repairMojibake(String answer) {
        boolean repairable = hasMojibakeMarker(answer)
                && StandardCharsets.ISO_8859_1.newEncoder().canEncode(answer);
        return repairable ? repairedMojibake(answer) : answer;
    }

    private String repairedMojibake(String answer) {
        String repaired = new String(
                answer.getBytes(StandardCharsets.ISO_8859_1),
                StandardCharsets.UTF_8
        );
        return mojibakeScore(repaired) < mojibakeScore(answer) ? repaired : answer;
    }

    private boolean hasMojibakeMarker(String answer) {
        return answer.contains("\u00C3") || answer.contains("\u00C2");
    }

    private int mojibakeScore(String answer) {
        int score = 0;

        for (char character : answer.toCharArray()) {
            score += isMojibakeCharacter(character) ? 1 : 0;
        }

        return score;
    }

    private boolean isMojibakeCharacter(char character) {
        return character == '\u00C3'
                || character == '\u00C2'
                || character == '\uFFFD';
    }

    private Optional<AssistantList> semicolonList(String answer) {
        return answer.contains(";") ? parsedSemicolonList(answer) : Optional.empty();
    }

    private Optional<AssistantList> parsedSemicolonList(String answer) {
        List<String> parts = Arrays.stream(answer.split(";"))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();

        return parts.size() < MIN_SEMICOLON_ITEMS
                ? Optional.empty()
                : Optional.of(assistantList(parts));
    }

    private AssistantList assistantList(List<String> parts) {
        List<String> items = new ArrayList<>(parts);
        String title = "";
        int titleSeparator = titleSeparator(items.get(0));

        if (hasTitle(items.get(0), titleSeparator)) {
            title = cleanTitle(items.get(0).substring(0, titleSeparator));
            items.set(0, items.get(0).substring(titleSeparator + 1));
        }

        List<String> cleanItems = items.stream()
                .map(this::cleanItem)
                .filter(item -> !item.isBlank())
                .toList();
        return new AssistantList(title, cleanItems);
    }

    private int titleSeparator(String text) {
        return text.indexOf(':');
    }

    private boolean hasTitle(String text, int titleSeparator) {
        return titleSeparator > 0
                && titleSeparator < text.length() - 1
                && titleSeparator <= MAX_TITLE_LENGTH
                && !text.substring(0, titleSeparator).contains(".");
    }

    private String cleanTitle(String title) {
        return title.trim().replaceAll("[:.]+$", "");
    }

    private String cleanItem(String item) {
        String cleanItem = item.trim();
        return cleanItem.endsWith(".")
                ? cleanItem.substring(0, cleanItem.length() - 1)
                : cleanItem;
    }

    private boolean shouldFormatAsList(String answer, AssistantList list) {
        long listLikeItems = list.items().stream()
                .filter(this::looksLikeListItem)
                .count();
        return list.items().size() >= MIN_SEMICOLON_ITEMS
                && (hasListTitle(list) || listLikeItems == list.items().size())
                && answer.lines().count() == 1;
    }

    private boolean hasListTitle(AssistantList list) {
        return !list.title().isBlank();
    }

    private boolean looksLikeListItem(String item) {
        return item.contains("$")
                || item.contains(":")
                || item.contains("(")
                || item.contains("|")
                || item.contains(" por ")
                || item.length() > 15;
    }

    private String formattedList(AssistantList list) {
        String bullets = list.items().stream()
                .map(item -> "- " + item)
                .collect(Collectors.joining("\n"));

        return list.title().isBlank() ? bullets : list.title() + ":\n" + bullets;
    }

    private record AssistantList(
            String title,
            List<String> items
    ) {
    }
}
