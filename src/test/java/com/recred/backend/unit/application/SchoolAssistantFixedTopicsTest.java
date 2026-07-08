package com.recred.backend.unit.application;

import com.recred.backend.application.SchoolAssistantContentFormatter;
import com.recred.backend.application.SchoolAssistantFixedTopicResponses;
import com.recred.backend.application.SchoolAssistantFixedReply;
import com.recred.backend.application.SchoolAssistantFixedTopics;
import com.recred.backend.application.SchoolAssistantIntentDetector;
import com.recred.backend.application.SchoolAssistantPurchasePatternService;
import com.recred.backend.application.SchoolAssistantReplyBuilder;
import com.recred.backend.application.SchoolAssistantReplyFormatter;
import com.recred.backend.application.SchoolAssistantSuggestionFactory;
import com.recred.backend.application.SchoolAssistantTutorFactsFormatter;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.SchoolAssistantActionStatus;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.SchoolCalendarEvent;
import com.recred.backend.domain.models.TipoDiaEscolar;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolAssistantFixedTopicsTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 27, 12, 0);

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private ProductRepository productRepository;

    private SchoolAssistantFixedTopics fixedTopics;
    private UUID studentId;
    private UUID buffetId;
    private Optional<SchoolAssistantFixedReply> reply;
    private SchoolAssistantContext studentContext;
    private SchoolAssistantContext tutorContext;

    @BeforeEach
    void setUp() {
        fixedTopics = fixedTopics();
        studentId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
    }

    private SchoolAssistantFixedTopics fixedTopics() {
        SchoolAssistantReplyFormatter replyFormatter = new SchoolAssistantReplyFormatter();
        SchoolAssistantContentFormatter contentFormatter =
                new SchoolAssistantContentFormatter(productRepository, replyFormatter);
        SchoolAssistantSuggestionFactory suggestionFactory =
                new SchoolAssistantSuggestionFactory();
        SchoolAssistantReplyBuilder replyBuilder =
                new SchoolAssistantReplyBuilder(suggestionFactory);
        SchoolAssistantPurchasePatternService purchasePatternService =
                new SchoolAssistantPurchasePatternService(
                        purchaseRepository,
                        productRepository,
                        contentFormatter
                );
        SchoolAssistantFixedTopicResponses responses =
                new SchoolAssistantFixedTopicResponses(
                        purchaseRepository,
                        productRepository,
                        purchasePatternService,
                        replyFormatter,
                        contentFormatter,
                        new SchoolAssistantTutorFactsFormatter(),
                        replyBuilder
                );
        return new SchoolAssistantFixedTopics(
                new SchoolAssistantIntentDetector(),
                responses
        );
    }

    @Test
    void respond_WhenStudentAsksBalance_ShouldReturnBalanceReply() {
        givenStudentContextWithBalance(new BigDecimal("500.00"));

        whenRespondingToStudent("cuanto saldo tengo");

        thenReplyAnswerIs("Tenes $500.00 disponibles.");
        thenReplyIntentIs(SchoolAssistantIntent.BALANCE);
        thenReplyActionIs(SchoolAssistantActionType.CONSULTA_SALDO);
        thenProductAndPurchaseRepositoriesWereNotUsed();
    }

    @Test
    void respond_WhenStudentAsksRecentPurchases_ShouldReturnPurchaseHistory() {
        givenStudentContextWithBalance(new BigDecimal("5000.00"));
        Producto product = givenProduct("Sandwich", new BigDecimal("1200.00"));
        givenStudentPurchaseHistory(List.of(purchase(product, 1, EstadoCompra.ENTREGADO)));

        whenRespondingToStudent("mostrame mis ultimas compras");

        thenReplyContains("1x Sandwich");
        thenReplyContains("$1200.00");
        thenReplyIntentIs(SchoolAssistantIntent.RECENT_PURCHASES);
    }

    @Test
    void respond_WhenStudentAsksFrequentPurchases_ShouldReturnDetectedPatterns() {
        givenStudentContextWithBalance(new BigDecimal("5000.00"));
        Producto product = givenProduct("Agua", new BigDecimal("700.00"));
        givenStudentPurchaseHistory(List.of(
                purchase(product, 1, EstadoCompra.ENTREGADO, NOW.minusDays(3)),
                purchase(product, 1, EstadoCompra.ENTREGADO, NOW.minusDays(2)),
                purchase(product, 1, EstadoCompra.ENTREGADO, NOW.minusDays(1))
        ));
        givenActiveProduct(product);

        whenRespondingToStudent("que compro seguido");

        thenReplyContains("Tus compras frecuentes");
        thenReplyContains("1x Agua");
        thenReplyContains("3 veces");
        thenReplyActionIs(SchoolAssistantActionType.CONSULTA_COMPRAS_FRECUENTES);
    }

    @Test
    void respond_WhenStudentAsksEvents_ShouldReturnEvents() {
        givenStudentContextWithEvents(List.of(event("Acto escolar")));

        whenRespondingToStudent("eventos");

        thenReplyContains("Eventos proximos");
        thenReplyContains("Acto escolar");
        thenReplyIntentIs(SchoolAssistantIntent.EVENTS);
    }

    @Test
    void respond_WhenStudentAsksMenu_ShouldReturnAvailableProductsFromLastBuffet() {
        givenStudentContextWithBalance(new BigDecimal("5000.00"));
        Producto previousProduct = givenProduct("Compra anterior", new BigDecimal("900.00"));
        Producto availableProduct = givenAvailableProduct("Tarta", new BigDecimal("1200.00"));
        Producto unavailableProduct = givenProduct("Jugo", new BigDecimal("800.00"));
        unavailableProduct.setStockActual(0);
        givenStudentPurchaseHistory(List.of(
                purchase(previousProduct, 1, EstadoCompra.ENTREGADO)
        ));
        when(productRepository.findAllActiveWithStock(buffetId))
                .thenReturn(List.of(availableProduct, unavailableProduct));

        whenRespondingToStudent("menu");

        thenReplyContains("Productos disponibles");
        thenReplyContains("Tarta");
        thenReplyDoesNotContain("Jugo");
        thenReplyActionIs(SchoolAssistantActionType.CONSULTA_MENU);
    }

    @Test
    void respond_WhenStudentAsksPendingOrders_ShouldReturnActivePurchases() {
        givenStudentContextWithBalance(new BigDecimal("5000.00"));
        Producto product = givenProduct("Alfajor", new BigDecimal("700.00"));
        givenStudentPurchaseHistory(List.of(
                purchase(product, 1, EstadoCompra.PENDIENTE),
                purchase(product, 1, EstadoCompra.ENTREGADO)
        ));

        whenRespondingToStudent("pedidos pendientes");

        thenReplyContains("Tus pedidos pendientes");
        thenReplyContains("1x Alfajor");
        thenReplyActionIs(SchoolAssistantActionType.CONSULTA_PEDIDOS);
    }

    @Test
    void respond_WhenStudentAsksWithdrawalCode_ShouldReturnFirstPendingCode() {
        givenStudentContextWithBalance(new BigDecimal("5000.00"));
        Producto product = givenProduct("Yogur", new BigDecimal("900.00"));
        Purchase purchase = purchase(product, 1, EstadoCompra.LISTO);
        purchase.setWithdrawalCode("ABC123");
        givenStudentPurchaseHistory(List.of(purchase));

        whenRespondingToStudent("codigo de retiro");

        thenReplyAnswerIs("Tu codigo de retiro es ABC123.");
        thenReplyActionIs(SchoolAssistantActionType.CONSULTA_CODIGO_RETIRO);
    }

    @Test
    void respond_WhenKiosqueroAsksStock_ShouldReturnBuffetStock() {
        Producto product = givenAvailableProduct("Sandwich", new BigDecimal("1500.00"));
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(product));

        whenRespondingToKiosquero("stock");

        thenReplyContains("Stock actual");
        thenReplyContains("Sandwich");
        thenReplyIntentIs(SchoolAssistantIntent.STOCK);
    }

    @Test
    void respond_WhenKiosqueroAsksPendingOrders_ShouldReturnBuffetOrders() {
        Producto product = givenProduct("Pizza", new BigDecimal("1300.00"));
        when(purchaseRepository.findByBuffetAndStatuses(eq(buffetId), any(), eq(3)))
                .thenReturn(List.of(purchase(product, 2, EstadoCompra.PENDIENTE)));

        whenRespondingToKiosquero("pedidos pendientes");

        thenReplyContains("Pedidos pendientes");
        thenReplyContains("2x Pizza");
        thenReplyActionIs(SchoolAssistantActionType.CONSULTA_PEDIDOS);
    }

    @Test
    void respond_WhenTutorAsksChildren_ShouldReturnChildrenFacts() {
        givenTutorContextWithFacts(List.of(tutorChildFact()));

        whenRespondingToTutor("mostrame mis hijos");

        thenReplyContains("Hijos asociados");
        thenReplyContains("Mateo Perez");
        thenReplyActionIs(SchoolAssistantActionType.CONSULTA_HIJOS);
    }

    @Test
    void respond_WhenTutorAsksBudgets_ShouldReturnBudgetFacts() {
        givenTutorContextWithFacts(List.of(tutorChildFact()));

        whenRespondingToTutor("presupuestos de mis hijos");

        thenReplyContains("Presupuestos");
        thenReplyContains("2000.00");
        thenReplyActionIs(SchoolAssistantActionType.CONSULTA_PRESUPUESTOS);
    }

    @Test
    void respond_WhenPurchaseMessageArrives_ShouldReturnEmpty() {
        givenStudentContextWithBalance(new BigDecimal("5000.00"));

        whenRespondingToStudent("confirmar");

        thenReplyIsEmpty();
        thenProductAndPurchaseRepositoriesWereNotUsed();
    }

    @Test
    void respond_WhenUnknownStudentMessageArrives_ShouldReturnEmpty() {
        givenStudentContextWithBalance(new BigDecimal("5000.00"));

        whenRespondingToStudent("necesito otra cosa");

        thenReplyIsEmpty();
    }

    private void givenStudentContextWithBalance(BigDecimal balance) {
        studentContext = studentContext(balance, List.of());
    }

    private void givenStudentContextWithEvents(List<SchoolCalendarEvent> events) {
        studentContext = studentContext(new BigDecimal("5000.00"), events);
    }

    private Producto givenProduct(String name, BigDecimal price) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre(name);
        product.setPrecio(price);
        product.setRequierePreparacion(false);
        product.setStockActual(10);
        return product;
    }

    private Producto givenAvailableProduct(String name, BigDecimal price) {
        Producto product = givenProduct(name, price);
        product.setStockActual(5);
        return product;
    }

    private void givenActiveProduct(Producto product) {
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
    }

    private void givenStudentPurchaseHistory(List<Purchase> purchases) {
        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(purchases);
    }

    private void givenTutorContextWithFacts(List<String> facts) {
        tutorContext = new SchoolAssistantContext(
                null,
                Optional.empty(),
                List.of(),
                List.of(),
                List.of(),
                SchoolAssistantRole.TUTOR,
                UUID.randomUUID(),
                null,
                facts,
                List.of()
        );
    }

    private void whenRespondingToStudent(String message) {
        reply = fixedTopics.respond(message, studentContext, List.of());
    }

    private void whenRespondingToKiosquero(String message) {
        reply = fixedTopics.respond(message, kiosqueroContext(), List.of());
    }

    private void whenRespondingToTutor(String message) {
        reply = fixedTopics.respond(message, tutorContext, List.of());
    }

    private void thenReplyAnswerIs(String expectedAnswer) {
        assertEquals(expectedAnswer, presentReply().answer());
    }

    private void thenReplyContains(String expectedText) {
        assertTrue(presentReply().answer().contains(expectedText));
    }

    private void thenReplyDoesNotContain(String unexpectedText) {
        assertFalse(presentReply().answer().contains(unexpectedText));
    }

    private void thenReplyIntentIs(SchoolAssistantIntent expectedIntent) {
        assertEquals(expectedIntent, presentReply().intent());
        assertEquals(expectedIntent.name(), presentReply().metadata().get("intencionChatbot"));
    }

    private void thenReplyActionIs(SchoolAssistantActionType expectedActionType) {
        assertEquals(expectedActionType, presentReply().action().type());
        assertEquals(SchoolAssistantActionStatus.INFORMATIVA, presentReply().action().status());
    }

    private void thenReplyIsEmpty() {
        assertTrue(reply.isEmpty());
    }

    private void thenProductAndPurchaseRepositoriesWereNotUsed() {
        verifyNoInteractions(productRepository, purchaseRepository);
    }

    private SchoolAssistantFixedReply presentReply() {
        assertTrue(reply.isPresent());
        return reply.orElseThrow();
    }

    private SchoolAssistantContext studentContext(
            BigDecimal balance,
            List<SchoolCalendarEvent> events
    ) {
        return new SchoolAssistantContext(
                studentId,
                Optional.ofNullable(balance),
                List.of(),
                List.of(),
                events
        );
    }

    private SchoolAssistantContext kiosqueroContext() {
        return new SchoolAssistantContext(
                null,
                Optional.empty(),
                List.of(),
                List.of(),
                List.of(),
                SchoolAssistantRole.KIOSQUERO,
                UUID.randomUUID(),
                buffetId,
                List.of(),
                List.of()
        );
    }

    private SchoolCalendarEvent event(String description) {
        return new SchoolCalendarEvent(
                LocalDate.of(2026, 5, 29),
                TipoDiaEscolar.JORNADA_ESPECIAL,
                description
        );
    }

    private Purchase purchase(Producto product, int quantity, EstadoCompra status) {
        return purchase(product, quantity, status, NOW);
    }

    private Purchase purchase(
            Producto product,
            int quantity,
            EstadoCompra status,
            LocalDateTime dateTime
    ) {
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setTotalAmount(product.getPrecio().multiply(BigDecimal.valueOf(quantity)));
        purchase.setStatus(status);
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setDate(dateTime);
        purchase.setItems(Map.of(product, quantity));
        purchase.setBuffetId(buffetId);
        return purchase;
    }

    private String tutorChildFact() {
        return "hijo=Mateo Perez|id=11111111-1111-1111-1111-111111111111"
                + "|saldo=900.00|grado=2do B"
                + "|presupuesto=monto=2000.00,periodo=MENSUAL,fechaInicio=2026-06-07"
                + "|restriccionesSalud=[Sin TACC]"
                + "|productosBloqueados=[Gaseosa]";
    }
}
