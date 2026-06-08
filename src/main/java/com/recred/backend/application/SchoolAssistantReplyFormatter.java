package com.recred.backend.application;

import java.util.List;
import java.util.stream.Collectors;

class SchoolAssistantReplyFormatter {

    private static final int TEXT_LIST_ITEMS_TO_SHOW = 8;

    String formattedItems(List<String> items) {
        List<String> visibleItems = items.stream()
                .limit(TEXT_LIST_ITEMS_TO_SHOW)
                .toList();
        String bullets = visibleItems.stream()
                .map(item -> "- " + item)
                .collect(Collectors.joining("\n"));
        return bullets + remainingItemsText(items.size(), visibleItems.size());
    }

    String countText(int count, String singular, String plural) {
        return count + " " + (count == 1 ? singular : plural);
    }

    private String remainingItemsText(int totalItems, int visibleItems) {
        return totalItems > visibleItems
                ? "\nY " + (totalItems - visibleItems) + " mas en el detalle."
                : "";
    }
}
