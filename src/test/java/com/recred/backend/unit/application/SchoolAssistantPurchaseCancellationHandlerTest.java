package com.recred.backend.application;

import com.recred.backend.domain.exceptions.InvalidPurchaseStatusTransitionException;
import com.recred.backend.domain.exceptions.PurchaseAccessDeniedException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.SchoolAssistantActionStatus;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolAssistantPurchaseCancellationHandlerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 1, 10, 0);

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CancelPresentialPurchase cancelPresentialPurchase;

    private SchoolAssistantPurchaseCancellationHandler handler;
    private UUID studentId;
    private UUID buffetId;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        handler = new SchoolAssistantPurchaseCancellationHandler(
                purchaseRepository,
                cancelPresentialPurchase,
                new SchoolAssistantContentFormatter(
                        productRepository,
                        new SchoolAssistantReplyFormatter()),
                new SchoolAssistantReplyBuilder(new SchoolAssistantSuggestionFactory())
        );
        studentId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
    }

    @Test
    void cancelPurchaseReplyShouldUseNestedExecutedAdvancePurchaseActionMetadata() {
        UUID purchaseId = UUID.randomUUID();
        when(cancelPresentialPurchase.execute(argThat(request ->
                request.purchaseId().equals(purchaseId)
                        && request.studentId().equals(studentId))))
                .thenReturn(cancelledResponse(purchaseId));

        SchoolAssistantFixedReply reply = handler.cancelPurchaseReply(
                context(),
                List.of(assistantMessage(nestedExecutedAdvancePurchaseMetadata(purchaseId)))
        );

        assertEquals("Listo, cancele tu compra y te devolvi $1500.00 en creditos.",
                reply.answer());
        assertEquals(SchoolAssistantIntent.CANCEL_PURCHASE, reply.intent());
        assertEquals(SchoolAssistantActionType.CANCELACION_COMPRA, reply.action().type());
        assertEquals(SchoolAssistantActionStatus.EJECUTADA, reply.action().status());
        assertEquals(purchaseId, reply.action().purchaseId());
        assertEquals(EstadoCompra.CANCELADO, reply.action().purchaseStatus());
        assertEquals("RET-123", reply.action().withdrawalCode());
        assertEquals(new BigDecimal("1500.00"), reply.action().total());
        verifyNoInteractions(purchaseRepository);
    }

    @Test
    void cancelPurchaseReplyShouldMapAccessDeniedFailure() {
        UUID purchaseId = UUID.randomUUID();
        when(cancelPresentialPurchase.execute(argThat(request ->
                request.purchaseId().equals(purchaseId))))
                .thenThrow(new PurchaseAccessDeniedException(purchaseId));

        SchoolAssistantFixedReply reply = handler.cancelPurchaseReply(
                context(),
                List.of(assistantMessage(createdPurchaseMetadata(purchaseId)))
        );

        assertEquals("Esa compra no corresponde a tu usuario.", reply.answer());
        assertEquals(SchoolAssistantActionStatus.FALLIDA, reply.action().status());
        assertEquals(purchaseId.toString(), reply.action().data().get("compraId"));
    }

    @Test
    void cancelPurchaseReplyShouldMapInvalidStatusFailure() {
        UUID purchaseId = UUID.randomUUID();
        when(cancelPresentialPurchase.execute(argThat(request ->
                request.purchaseId().equals(purchaseId))))
                .thenThrow(new InvalidPurchaseStatusTransitionException(
                        EstadoCompra.ENTREGADO,
                        EstadoCompra.CANCELADO));

        SchoolAssistantFixedReply reply = handler.cancelPurchaseReply(
                context(),
                List.of(assistantMessage(createdPurchaseMetadata(purchaseId)))
        );

        assertEquals("Esa compra ya no se puede cancelar desde el asistente.",
                reply.answer());
        assertEquals(SchoolAssistantActionStatus.FALLIDA, reply.action().status());
    }

    @Test
    void cancelPurchaseReplyShouldPreserveCancellationWindowFailureMessage() {
        UUID purchaseId = UUID.randomUUID();
        String message = "No se puede cancelar con menos de una hora de anticipacion.";
        when(cancelPresentialPurchase.execute(argThat(request ->
                request.purchaseId().equals(purchaseId))))
                .thenThrow(new IllegalArgumentException(message));

        SchoolAssistantFixedReply reply = handler.cancelPurchaseReply(
                context(),
                List.of(assistantMessage(createdPurchaseMetadata(purchaseId)))
        );

        assertEquals(message, reply.answer());
        assertEquals(SchoolAssistantActionStatus.FALLIDA, reply.action().status());
    }

    @Test
    void cancelPurchaseReplyShouldUseGenericFailureForUnexpectedIllegalArgument() {
        UUID purchaseId = UUID.randomUUID();
        when(cancelPresentialPurchase.execute(argThat(request ->
                request.purchaseId().equals(purchaseId))))
                .thenThrow(new IllegalArgumentException("Unexpected error"));

        SchoolAssistantFixedReply reply = handler.cancelPurchaseReply(
                context(),
                List.of(assistantMessage(createdPurchaseMetadata(purchaseId)))
        );

        assertEquals("No pude cancelar esa compra.", reply.answer());
        assertEquals(SchoolAssistantActionStatus.FALLIDA, reply.action().status());
    }

    @Test
    void cancelPurchaseReplyShouldIgnoreInvalidRecentMetadataAndNonCancelableHistory() {
        UUID purchaseId = UUID.randomUUID();
        Purchase withoutId = purchase(null, EstadoCompra.PENDIENTE);
        Purchase delivered = purchase(purchaseId, EstadoCompra.ENTREGADO);
        when(purchaseRepository.findHistorialByAlumno(studentId))
                .thenReturn(Arrays.asList(null, withoutId, delivered));

        SchoolAssistantFixedReply reply = handler.cancelPurchaseReply(
                context(),
                List.of(
                        assistantMessage(null),
                        assistantMessage(Map.of()),
                        userMessage(createdPurchaseMetadata(UUID.randomUUID())),
                        assistantMessage(createdPurchaseMetadata("invalid-uuid"))
                )
        );

        assertEquals("No encontre compras pendientes que puedas cancelar.", reply.answer());
        assertEquals(SchoolAssistantActionStatus.FALLIDA, reply.action().status());
        assertTrue(reply.action().data().isEmpty());
        verifyNoInteractions(cancelPresentialPurchase);
    }

    @Test
    void cancelPurchaseReplyShouldShowOnlyFirstCancelablePurchasesInAnswer() {
        Producto product = product();
        List<Purchase> purchases = List.of(
                purchase(UUID.randomUUID(), EstadoCompra.PENDIENTE, product, 1),
                purchase(UUID.randomUUID(), EstadoCompra.PENDIENTE, product, 2),
                purchase(UUID.randomUUID(), EstadoCompra.PENDIENTE, product, 3),
                purchase(UUID.randomUUID(), EstadoCompra.PENDIENTE, product, 4)
        );
        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(purchases);

        SchoolAssistantFixedReply reply = handler.cancelPurchaseReply(context(), List.of());

        assertTrue(reply.answer().contains("Tenes mas de una compra pendiente"));
        assertTrue(reply.answer().contains("1x Alfajor"));
        assertTrue(reply.answer().contains("2x Alfajor"));
        assertTrue(reply.answer().contains("3x Alfajor"));
        assertTrue(!reply.answer().contains("4x Alfajor"));
        assertEquals(4, ((List<?>) reply.action().data().get("comprasCancelables")).size());
        verifyNoInteractions(cancelPresentialPurchase);
    }

    private SchoolAssistantContext context() {
        return new SchoolAssistantContext(
                studentId,
                Optional.empty(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private ChatMessage assistantMessage(Map<String, Object> metadata) {
        return message(RolChatMensaje.ASISTENTE_IA, metadata);
    }

    private ChatMessage userMessage(Map<String, Object> metadata) {
        return message(RolChatMensaje.USUARIO, metadata);
    }

    private ChatMessage message(RolChatMensaje role, Map<String, Object> metadata) {
        return new ChatMessage(sessionId, role, "mensaje", metadata, NOW);
    }

    private Map<String, Object> createdPurchaseMetadata(UUID purchaseId) {
        return createdPurchaseMetadata(purchaseId.toString());
    }

    private Map<String, Object> createdPurchaseMetadata(String purchaseId) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("estadoAccionChatbot", "EJECUTADA");
        metadata.put("compraId", purchaseId);
        return metadata;
    }

    private Map<String, Object> nestedExecutedAdvancePurchaseMetadata(UUID purchaseId) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("tipo", SchoolAssistantActionType.COMPRA_ANTICIPADA.name());
        action.put("estado", "EJECUTADA");
        action.put("compraId", purchaseId.toString());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("accion", action);
        return metadata;
    }

    private CancelPresentialPurchaseResponse cancelledResponse(UUID purchaseId) {
        return new CancelPresentialPurchaseResponse(
                purchaseId,
                studentId,
                buffetId,
                EstadoCompra.CANCELADO,
                "RET-123",
                new BigDecimal("1500.00")
        );
    }

    private Purchase purchase(UUID purchaseId, EstadoCompra status) {
        Purchase purchase = new Purchase();
        purchase.setId(purchaseId);
        purchase.setStatus(status);
        return purchase;
    }

    private Purchase purchase(
            UUID purchaseId,
            EstadoCompra status,
            Producto product,
            int quantity
    ) {
        Purchase purchase = purchase(purchaseId, status);
        purchase.setStudent(alumno());
        purchase.setBuffetId(buffetId);
        purchase.setDate(NOW);
        purchase.setTotalAmount(product.getPrecio().multiply(BigDecimal.valueOf(quantity)));
        purchase.setItems(Map.of(product, quantity));
        return purchase;
    }

    private Alumno alumno() {
        Alumno alumno = new Alumno();
        alumno.setId(studentId);
        return alumno;
    }

    private Producto product() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Alfajor");
        product.setPrecio(new BigDecimal("500.00"));
        return product;
    }
}
