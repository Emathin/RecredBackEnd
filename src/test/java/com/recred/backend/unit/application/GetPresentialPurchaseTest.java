package com.recred.backend.unit.application;

import com.recred.backend.application.GetPresentialPurchase;
import com.recred.backend.application.GetPresentialPurchaseRequest;
import com.recred.backend.application.GetPresentialPurchaseResponse;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPresentialPurchaseTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @InjectMocks
    private GetPresentialPurchase useCase;

    private UUID purchaseId;
    private Purchase purchase;

    @BeforeEach
    void setUp() {
        purchaseId = UUID.randomUUID();
        purchase = new Purchase();
        purchase.setId(purchaseId);
        purchase.setStatus(EstadoCompra.PENDIENTE);
    }

    @Test
    void execute_WhenPurchaseExists_ShouldReturnResponse() {
        GetPresentialPurchaseRequest request = new GetPresentialPurchaseRequest(purchaseId);

        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));

        GetPresentialPurchaseResponse response = useCase.execute(request);

        assertNotNull(response);
        assertEquals(purchaseId, response.id());
        assertEquals("PENDIENTE", response.status());
    }

    @Test
    void execute_WhenPurchaseDoesNotExist_ShouldThrowException() {
        GetPresentialPurchaseRequest request = new GetPresentialPurchaseRequest(purchaseId);

        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.empty());

        Executable execution = () -> useCase.execute(request);

        assertThrows(PurchaseNotFoundException.class, execution);
    }

    @Test
    void execute_WhenPurchaseHasRegularAndPromotionItems_ShouldGroupPromotionItems() {
        UUID promoId = UUID.randomUUID();
        Producto standalone = product("Agua", new BigDecimal("700.00"), false, null);
        Producto firstPromoItem = product("Cafe", new BigDecimal("800.00"), true, promoId);
        Producto secondPromoItem = product("Medialuna", new BigDecimal("600.00"), false, promoId);
        purchase.setTotalAmount(new BigDecimal("2100.00"));
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setDate(LocalDateTime.of(2026, 7, 1, 10, 0));
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setWithdrawalCode("RET-123");
        purchase.setPickupSlotDescription("Primer recreo");
        purchase.setPickupDate(LocalDate.of(2026, 7, 2));
        purchase.setPickupSlotStartTime(LocalTime.of(9, 0));
        purchase.setItems(Map.of(
                standalone, 1,
                firstPromoItem, 2,
                secondPromoItem, 2
        ));
        GetPresentialPurchaseRequest request = new GetPresentialPurchaseRequest(purchaseId);

        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));

        GetPresentialPurchaseResponse response = useCase.execute(request);

        assertEquals("CREDITOS", response.paymentMethod());
        assertEquals("ANTICIPADA", response.tipo());
        assertEquals("RET-123", response.withdrawalCode());
        assertEquals(2, response.items().size());
        GetPresentialPurchaseResponse.ItemResponse promoItem = response.items().stream()
                .filter(item -> item.productId().equals(promoId))
                .findFirst()
                .orElseThrow();
        assertEquals("Promo desayuno", promoItem.nombre());
        assertEquals(2, promoItem.quantity());
        assertEquals(new BigDecimal("1400.00"), promoItem.precio());
        assertEquals(true, promoItem.requierePreparacion());
        assertEquals(promoId, promoItem.promocionId());
    }

    @Test
    void execute_WhenPurchaseHasNullFields_ShouldReturnNullableResponseFields() {
        purchase.setStatus(null);
        purchase.setItems(null);
        GetPresentialPurchaseRequest request = new GetPresentialPurchaseRequest(purchaseId);

        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));

        GetPresentialPurchaseResponse response = useCase.execute(request);

        assertEquals(purchaseId, response.id());
        assertNull(response.status());
        assertNull(response.statusLabel());
        assertNull(response.paymentMethod());
        assertEquals(0, response.items().size());
    }

    private Producto product(
            String name,
            BigDecimal price,
            boolean requiresPreparation,
            UUID promotionId
    ) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre(name);
        product.setPrecio(price);
        product.setRequierePreparacion(requiresPreparation);
        product.setCupoMaximoDiario(10);
        product.setPromocionId(promotionId);
        product.setPromocionNombre(promotionId == null ? null : "Promo desayuno");
        return product;
    }
}
