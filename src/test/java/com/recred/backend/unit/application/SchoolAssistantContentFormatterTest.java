package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.SchoolCalendarEvent;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.recred.backend.domain.models.RecessTime.AFTER_HOURS;
import static com.recred.backend.domain.models.RecessTime.NOON;
import static com.recred.backend.domain.models.RecessTime.SECOND_RECESS;
import static com.recred.backend.domain.models.TipoDiaEscolar.CLASE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class SchoolAssistantContentFormatterTest {

    private static final UUID DUMMY_ID = UUID.randomUUID();
    private static final String SIN_ESTADO = "sin estado";
    private static final String SIN_FECHA = "sin fecha";
    private static final String ZERO_DOLLARS = "$0.00";

    private SchoolAssistantContentFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new SchoolAssistantContentFormatter(
                mock(ProductRepository.class),
                new SchoolAssistantReplyFormatter());
    }

    @Test
    void shouldFormatMoneyAndLabelsWithNullsAndAllRecesses() {
        final String money = whenFormatMoney(null);
        final String secondRecess = whenFormatRecessLabel(SECOND_RECESS);
        final String noon = whenFormatRecessLabel(NOON);
        final String afterHours = whenFormatRecessLabel(AFTER_HOURS);
        final String purchaseStatus = whenFormatPurchaseStatusLabel(null);

        thenResultIs(money, ZERO_DOLLARS);
        thenResultIs(secondRecess, "segundo recreo");
        thenResultIs(noon, "mediodia");
        thenResultIs(afterHours, "salida");
        thenResultIs(purchaseStatus, SIN_ESTADO);
    }

    @Test
    void shouldReturnEmptyItemListWhenPurchaseItemsAreNull() {
        Purchase purchase = givenPurchaseWithNullItems();

        Map<String, Object> result = whenFormatPurchaseData(purchase);

        thenItemListIsEmpty(result);
    }

    @Test
    void shouldFormatNullDateNullAmountAndSkipInvalidQuantities() {
        Purchase purchase = givenPurchaseWithInvalidItemsAndNullData();

        String result = whenFormatPurchaseList(purchase);

        thenResultContains(result, SIN_FECHA);
        thenResultContains(result, ZERO_DOLLARS);
        thenResultContains(result, SIN_ESTADO);
    }

    @Test
    void shouldUseFallbackDateWhenEventDateIsNull() {
        SchoolCalendarEvent event = givenEventWithNullDate();

        String result = whenFormatEventsList(event);

        thenResultContains(result, SIN_FECHA);
    }

    @Test
    void shouldFormatSubtotalFromUnitPrice() {
        PurchasePlan plan = givenPurchasePlanWithOneItem("Jugo", 2, "10.00");

        Map<String, Object> result = whenFormatPurchasePlanData(plan);

        thenSubtotalIs(result, "20.00");
    }

    private String whenFormatMoney(BigDecimal amount) {
        return formatter.money(amount);
    }

    private String whenFormatRecessLabel(RecessTime recessTime) {
        return formatter.recessLabel(recessTime);
    }

    private String whenFormatPurchaseStatusLabel(EstadoCompra status) {
        return formatter.purchaseStatusLabel(status);
    }

    private Purchase givenPurchaseWithNullItems() {
        Purchase purchase = new Purchase();
        purchase.setItems(null);
        return purchase;
    }

    private Map<String, Object> whenFormatPurchaseData(Purchase purchase) {
        return formatter.purchaseData(purchase);
    }

    private Purchase givenPurchaseWithInvalidItemsAndNullData() {
        Producto product = new Producto();
        product.setId(DUMMY_ID);
        product.setNombre("Alfajor");
        Purchase purchase = new Purchase();
        purchase.setItems(new HashMap<>(Map.of(product, 0)));
        purchase.setTotalAmount(null);
        purchase.setDate(null);
        purchase.setStatus(null);
        return purchase;
    }

    private String whenFormatPurchaseList(Purchase purchase) {
        return formatter.purchaseList(List.of(purchase));
    }

    private SchoolCalendarEvent givenEventWithNullDate() {
        return new SchoolCalendarEvent(null, CLASE, "Clase normal");
    }

    private String whenFormatEventsList(SchoolCalendarEvent event) {
        return formatter.eventsList(List.of(event));
    }

    private PurchasePlan givenPurchasePlanWithOneItem(String name, int quantity, String price) {
        PurchasePlanItem item = new PurchasePlanItem(DUMMY_ID, name, quantity, new BigDecimal(price));
        return new PurchasePlan(DUMMY_ID, DUMMY_ID, List.of(item), 1, null);
    }

    private Map<String, Object> whenFormatPurchasePlanData(PurchasePlan plan) {
        return formatter.purchasePlanData(plan);
    }

    private void thenResultIs(String actual, String expected) {
        assertEquals(expected, actual);
    }

    private void thenResultContains(String result, String expectedContent) {
        assertTrue(result.contains(expectedContent));
    }

    private void thenItemListIsEmpty(Map<String, Object> result) {
        assertEquals(List.of(), result.get("items"));
    }

    private void thenSubtotalIs(Map<String, Object> result, String expectedSubtotal) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertEquals(new BigDecimal(expectedSubtotal), items.get(0).get("subtotal"));
    }
}
