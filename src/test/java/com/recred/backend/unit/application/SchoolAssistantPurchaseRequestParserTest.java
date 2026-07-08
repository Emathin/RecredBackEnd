package com.recred.backend.application;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchoolAssistantPurchaseRequestParserTest {

    private SchoolAssistantPurchaseRequestParser parser;
    private SchoolAssistantContext context;
    private List<ChatMessage> recentMessages;
    private String message;
    private SchoolAssistantFixedTopicRequest request;

    @BeforeEach
    void setUp() {
        parser = new SchoolAssistantPurchaseRequestParser(
                new SchoolAssistantIntentDetector(),
                fixedClock()
        );
        recentMessages = List.of();
    }

    @Test
    void request_WhenMessageSelectsPurchaseDate_ShouldKeepContextAndSelectedDate() {
        givenStudentContext();
        givenDateSelectionMessage();

        whenParsingRequest();

        thenDateSelectionRequestWasBuilt();
    }

    @Test
    void request_WhenMessageSelectsRecess_ShouldKeepContextAndSelectedRecess() {
        givenStudentContext();
        givenRecessSelectionMessage();

        whenParsingRequest();

        thenRecessSelectionRequestWasBuilt();
    }

    private void givenStudentContext() {
        context = new SchoolAssistantContext(
                UUID.randomUUID(),
                Optional.of(new BigDecimal("1500.00")),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private void givenDateSelectionMessage() {
        message = "28/5";
    }

    private void givenRecessSelectionMessage() {
        message = "primer recreo";
    }

    private void whenParsingRequest() {
        request = parser.request(message, context, recentMessages);
    }

    private void thenDateSelectionRequestWasBuilt() {
        assertEquals(SchoolAssistantIntent.SELECT_PURCHASE_DATE, request.intent());
        assertEquals(LocalDate.of(2026, 5, 28),
                request.selectedPurchaseDate().orElseThrow());
        assertTrue(request.selectedRecessTime().isEmpty());
        assertSame(context, request.context());
        assertSame(recentMessages, request.recentMessages());
    }

    private void thenRecessSelectionRequestWasBuilt() {
        assertEquals(SchoolAssistantIntent.SELECT_RECESS, request.intent());
        assertEquals(RecessTime.FIRST_RECESS, request.selectedRecessTime().orElseThrow());
        assertTrue(request.selectedPurchaseDate().isEmpty());
        assertSame(context, request.context());
        assertSame(recentMessages, request.recentMessages());
    }

    private Clock fixedClock() {
        return Clock.fixed(
                Instant.parse("2026-05-27T12:00:00Z"),
                ZoneId.of("America/Argentina/Buenos_Aires")
        );
    }
}
