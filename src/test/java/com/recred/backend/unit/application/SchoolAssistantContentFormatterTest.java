package com.recred.backend.application;


import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.SchoolCalendarEvent;
import com.recred.backend.domain.models.TipoDiaEscolar;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class SchoolAssistantContentFormatterTest {

    private SchoolAssistantContentFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new SchoolAssistantContentFormatter(
                mock(ProductRepository.class),
                new SchoolAssistantReplyFormatter());
    }

    @Test
    void moneyAndLabels_ShouldFormatNullsAndAllRecesses() {
        assertEquals("$0.00", formatter.money(null));
        assertEquals("segundo recreo", formatter.recessLabel(RecessTime.SECOND_RECESS));
        assertEquals("mediodia", formatter.recessLabel(RecessTime.NOON));
        assertEquals("salida", formatter.recessLabel(RecessTime.AFTER_HOURS));
        assertEquals("sin estado", formatter.purchaseStatusLabel(null));
    }

    @Test
    void purchaseData_WhenItemsAreNull_ShouldUseEmptyItemList() {
        Purchase purchase = new Purchase();
        purchase.setItems(null);

        Map<String, Object> result = formatter.purchaseData(purchase);

        assertEquals(List.of(), result.get("items"));
    }

    @Test
    void purchaseList_ShouldFormatNullDateNullAmountAndSkipInvalidQuantities() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Alfajor");
        Purchase purchase = new Purchase();
        purchase.setItems(new HashMap<>(Map.of(product, 0)));
        purchase.setTotalAmount(null);
        purchase.setDate(null);
        purchase.setStatus(null);

        String result = formatter.purchaseList(List.of(purchase));

        assertTrue(result.contains("sin fecha"));
        assertTrue(result.contains("$0.00"));
        assertTrue(result.contains("sin estado"));
    }

    @Test
    void eventsList_WhenDateIsNull_ShouldUseFallbackDate() {
        SchoolCalendarEvent event = new SchoolCalendarEvent(
                null,
                TipoDiaEscolar.CLASE,
                "Clase normal");

        String result = formatter.eventsList(List.of(event));

        assertTrue(result.contains("sin fecha"));
    }

    @Test
    void purchasePlanItemList_ShouldFormatSubtotalFromUnitPrice() {
        PurchasePlanItem item = new PurchasePlanItem(
                UUID.randomUUID(),
                "Jugo",
                2,
                new BigDecimal("10.00"));

        Map<String, Object> result = formatter.purchasePlanData(new PurchasePlan(
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(item),
                1,
                null));

        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertEquals(new BigDecimal("20.00"), items.get(0).get("subtotal"));
    }
}
