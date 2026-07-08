package com.recred.backend.unit.application;

import com.recred.backend.application.SchoolAssistantMessageClassification;
import com.recred.backend.application.SchoolAssistantMessageClassifier;
import com.recred.backend.application.SchoolAssistantMessageType;
import com.recred.backend.application.SchoolAssistantIntentDetector;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchoolAssistantMessageClassifierTest {

    private SchoolAssistantMessageClassifier classifier;
    private String message;
    private SchoolAssistantMessageClassification classification;

    @BeforeEach
    void setUp() {
        classifier = new SchoolAssistantMessageClassifier(new SchoolAssistantIntentDetector());
    }

    @Test
    void classify_WhenMessageIsQuestion_ShouldReturnQuestionType() {
        givenBalanceQuestion();

        whenClassifyingMessage();

        thenMessageWasClassifiedAsQuestion(SchoolAssistantIntent.BALANCE);
    }

    @Test
    void classify_WhenMessageIsUnknown_ShouldReturnQuestionType() {
        givenUnknownQuestion();

        whenClassifyingMessage();

        thenMessageWasClassifiedAsQuestion(SchoolAssistantIntent.UNKNOWN);
    }

    @Test
    void classify_WhenMessageStartsPurchaseFlow_ShouldReturnPurchaseType() {
        givenRepeatLastPurchaseMessage();

        whenClassifyingMessage();

        thenMessageWasClassifiedAsPurchase(SchoolAssistantIntent.REPEAT_LAST_PURCHASE);
    }

    @Test
    void classify_WhenMessageContinuesPurchaseFlow_ShouldReturnPurchaseType() {
        givenPurchaseRecessSelection();

        whenClassifyingMessage();

        thenMessageWasClassifiedAsPurchase(SchoolAssistantIntent.SELECT_RECESS);
    }

    @Test
    void classify_WhenMessageCancelsDraftAction_ShouldReturnPurchaseType() {
        givenDraftCancellationMessage();

        whenClassifyingMessage();

        thenMessageWasClassifiedAsPurchase(SchoolAssistantIntent.CANCEL_ACTION);
    }

    @Test
    void classify_WhenMessageCancelsPurchase_ShouldReturnPurchaseCancellationType() {
        givenPurchaseCancellationMessage();

        whenClassifyingMessage();

        thenMessageWasClassifiedAsPurchaseCancellation();
    }

    private void givenBalanceQuestion() {
        message = "cuanto saldo tengo";
    }

    private void givenUnknownQuestion() {
        message = "necesito ayuda con otra cosa";
    }

    private void givenRepeatLastPurchaseMessage() {
        message = "repeti mi ultima compra";
    }

    private void givenPurchaseRecessSelection() {
        message = "primer recreo";
    }

    private void givenDraftCancellationMessage() {
        message = "cancelar";
    }

    private void givenPurchaseCancellationMessage() {
        message = "cancelar mi compra";
    }

    private void whenClassifyingMessage() {
        classification = classifier.classify(message);
    }

    private void thenMessageWasClassifiedAsQuestion(SchoolAssistantIntent expectedIntent) {
        assertEquals(SchoolAssistantMessageType.QUESTION, classification.type());
        assertEquals(expectedIntent, classification.intent());
        assertTrue(classification.isQuestion());
    }

    private void thenMessageWasClassifiedAsPurchase(SchoolAssistantIntent expectedIntent) {
        assertEquals(SchoolAssistantMessageType.PURCHASE, classification.type());
        assertEquals(expectedIntent, classification.intent());
        assertTrue(classification.isPurchase());
    }

    private void thenMessageWasClassifiedAsPurchaseCancellation() {
        assertEquals(SchoolAssistantMessageType.PURCHASE_CANCELLATION, classification.type());
        assertEquals(SchoolAssistantIntent.CANCEL_PURCHASE, classification.intent());
        assertTrue(classification.isPurchaseCancellation());
    }
}
