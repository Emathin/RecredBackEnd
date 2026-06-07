package com.recred.backend.application;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolAssistantFixedTopicsTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 27, 12, 0);

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProcessPresentialPurchase processPresentialPurchase;

    private SchoolAssistantFixedTopics fixedTopics;
    private UUID studentId;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        fixedTopics = new SchoolAssistantFixedTopics(
                purchaseRepository,
                productRepository,
                processPresentialPurchase
        );
        studentId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
    }

    @Test
    void respondsBalanceTopic() {
        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "cuanto saldo tengo",
                studentContext(new BigDecimal("500.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals("Tenes $500.00 disponibles.", reply.orElseThrow().answer());
        assertEquals("BALANCE", reply.orElseThrow().metadata().get("intencionChatbot"));
        verifyNoInteractions(purchaseRepository, productRepository, processPresentialPurchase);
    }

    @Test
    void respondsRecentPurchasesTopic() {
        Producto product = product("Sandwich", new BigDecimal("1200.00"));
        Purchase purchase = purchase(product, 1, NOW.minusHours(2));

        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of(purchase));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "mostrame mis ultimas compras",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertTrue(reply.orElseThrow().answer().contains("1x Sandwich"));
        assertTrue(reply.orElseThrow().answer().contains("$1200.00"));
        assertTrue(reply.orElseThrow().answer().contains("ENTREGADO"));
    }

    @Test
    void createsPendingActionForLastPurchase() {
        Producto product = product("Agua", new BigDecimal("700.00"));
        Purchase purchase = purchase(product, 2, NOW.minusDays(1));

        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of(purchase));
        when(productRepository.findActiveById(product.getId())).thenReturn(Optional.of(product));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "repeti mi ultima compra",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertTrue(reply.orElseThrow().answer().contains("2x Agua"));
        assertTrue(reply.orElseThrow().answer().contains("Escribi confirmar"));
        assertEquals("PENDIENTE", reply.orElseThrow().metadata().get("estadoAccionChatbot"));

        Map<?, ?> action = (Map<?, ?>) reply.orElseThrow()
                .metadata()
                .get("accionPendienteChatbot");
        List<?> items = (List<?>) action.get("items");
        Map<?, ?> item = (Map<?, ?>) items.get(0);
        assertEquals(product.getId().toString(), item.get("productoId"));
        assertEquals(2, item.get("cantidad"));
    }

    @Test
    void confirmsPendingPurchase() {
        Producto product = product("Yogur", new BigDecimal("900.00"));
        Purchase savedPurchase = purchase(product, 1, NOW);
        savedPurchase.setId(UUID.randomUUID());

        when(productRepository.findActiveById(product.getId())).thenReturn(Optional.of(product));
        when(processPresentialPurchase.execute(
                eq(studentId),
                argThat(cart -> cart.containsValue(1))
        )).thenReturn(savedPurchase);

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "confirmar",
                studentContext(new BigDecimal("5000.00")),
                List.of(assistantMessage(pendingMetadata(product.getId(), 1)))
        );

        assertTrue(reply.isPresent());
        assertTrue(reply.orElseThrow().answer().contains("Compra realizada"));
        assertEquals("EJECUTADA", reply.orElseThrow().metadata().get("estadoAccionChatbot"));
        assertEquals(savedPurchase.getId().toString(), reply.orElseThrow().metadata().get(
                "compraId"
        ));
        verify(processPresentialPurchase).execute(
                eq(studentId),
                argThat(cart -> cart.containsKey(product) && cart.containsValue(1))
        );
    }

    @Test
    void createsPendingActionForUsualPurchasePattern() {
        Producto product = product("Barrita", new BigDecimal("650.00"));
        Purchase firstPurchase = purchase(product, 1, NOW.minusDays(1));
        Purchase secondPurchase = purchase(product, 1, NOW.minusDays(3));

        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of(
                firstPurchase,
                secondPurchase
        ));
        when(productRepository.findActiveById(product.getId())).thenReturn(Optional.of(product));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "comprame lo de siempre",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertTrue(reply.orElseThrow().answer().contains("Tu compra habitual detectada"));
        assertTrue(reply.orElseThrow().answer().contains("1x Barrita"));
        assertEquals("PENDIENTE", reply.orElseThrow().metadata().get("estadoAccionChatbot"));
    }

    private SchoolAssistantContext studentContext(BigDecimal balance) {
        return new SchoolAssistantContext(
                studentId,
                Optional.ofNullable(balance),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private Producto product(String name, BigDecimal price) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre(name);
        product.setPrecio(price);
        product.setRequierePreparacion(false);
        return product;
    }

    private Purchase purchase(Producto product, int quantity, LocalDateTime dateTime) {
        Purchase purchase = new Purchase();
        purchase.setStudent(null);
        purchase.setTotalAmount(product.getPrecio().multiply(BigDecimal.valueOf(quantity)));
        purchase.setStatus(EstadoCompra.ENTREGADO);
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setDate(dateTime);
        purchase.setItems(Map.of(product, quantity));
        return purchase;
    }

    private ChatMessage assistantMessage(Map<String, Object> metadata) {
        return new ChatMessage(
                sessionId,
                RolChatMensaje.ASISTENTE_IA,
                "draft",
                metadata,
                NOW
        );
    }

    private Map<String, Object> pendingMetadata(UUID productId, int quantity) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("estadoAccionChatbot", "PENDIENTE");
        metadata.put("accionPendienteChatbot", pendingAction(productId, quantity));
        return metadata;
    }

    private Map<String, Object> pendingAction(UUID productId, int quantity) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("tipo", "COMPRA_RAPIDA");
        action.put("alumnoId", studentId.toString());
        action.put("items", List.of(pendingItem(productId, quantity)));
        return action;
    }

    private Map<String, Object> pendingItem(UUID productId, int quantity) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("productoId", productId.toString());
        item.put("cantidad", quantity);
        return item;
    }
}
