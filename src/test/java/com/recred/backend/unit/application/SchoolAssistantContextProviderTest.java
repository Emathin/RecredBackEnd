package com.recred.backend.unit.application;

import com.recred.backend.application.KiosqueroAssistantContextBuilder;
import com.recred.backend.application.KiosqueroAssistantContextProvider;
import com.recred.backend.application.SchoolAssistantConversationOwnerType;
import com.recred.backend.application.SchoolAssistantResolvedContext;
import com.recred.backend.application.SchoolAssistantRoleCatalog;
import com.recred.backend.application.StudentAssistantContextBuilder;
import com.recred.backend.application.StudentAssistantContextProvider;
import com.recred.backend.application.TutorAssistantContextBuilder;
import com.recred.backend.application.TutorAssistantContextProvider;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.KiosqueroNotFoundException;
import com.recred.backend.domain.exceptions.TutorNoEncontradoException;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoPagoExterno;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantPayment;
import com.recred.backend.domain.models.SchoolAssistantPurchase;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.SchoolCalendarEvent;
import com.recred.backend.domain.models.TipoDiaEscolar;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPaymentRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPurchaseRepository;
import com.recred.backend.domain.repositories.SchoolCalendarRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolAssistantContextProviderTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-27T12:00:00Z"),
            ZoneOffset.UTC
    );
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 27, 12, 0);
    private static final String COGNITO_SUBJECT = "subject";

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private SchoolAssistantPaymentRepository paymentRepository;

    @Mock
    private SchoolAssistantPurchaseRepository assistantPurchaseRepository;

    @Mock
    private SchoolCalendarRepository schoolCalendarRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private PresupuestoRepository presupuestoRepository;

    @Mock
    private KiosqueroRepository kiosqueroRepository;

    @Mock
    private ProductRepository productRepository;

    private StudentAssistantContextProvider studentProvider;
    private TutorAssistantContextProvider tutorProvider;
    private KiosqueroAssistantContextProvider kiosqueroProvider;
    private UUID userId;
    private UUID studentId;
    private UUID tutorId;
    private UUID buffetId;
    private Usuario user;
    private Tutor tutor;
    private SchoolAssistantPayment payment;
    private SchoolAssistantPurchase assistantPurchase;
    private SchoolCalendarEvent event;
    private Purchase purchase;
    private Producto product;
    private SchoolAssistantResolvedContext resolvedContext;
    private Executable execution;

    @BeforeEach
    void setUp() {
        SchoolAssistantRoleCatalog roleCatalog = new SchoolAssistantRoleCatalog();
        studentProvider = new StudentAssistantContextProvider(
                alumnoRepository,
                studentContextBuilder(roleCatalog)
        );
        tutorProvider = new TutorAssistantContextProvider(
                tutorRepository,
                tutorContextBuilder(roleCatalog)
        );
        kiosqueroProvider = new KiosqueroAssistantContextProvider(
                kiosqueroRepository,
                kiosqueroContextBuilder(roleCatalog)
        );
        userId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        tutorId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
        user = user(userId);
    }

    @Test
    void studentProvider_WhenUserIsStudent_ShouldBuildContextAndUseStudentOwner() {
        givenUserIdBelongsToStudent();
        givenStudentContextData();

        whenResolvingStudentContext();

        thenResolvedOwnerIsStudent(studentId);
        thenStudentContextWasBuilt();
    }

    @Test
    void studentProvider_WhenStudentCannotBeResolved_ShouldThrowException() {
        givenStudentCannotBeResolved();

        whenResolvingStudentContextFails();

        thenStudentNotFoundExceptionIsThrown();
    }

    @Test
    void tutorProvider_WhenTutorExists_ShouldBuildContextAndUseUserOwner() {
        givenTutorExists();
        givenTutorContextData();

        whenResolvingTutorContext();

        thenResolvedOwnerIsUser(userId);
        thenTutorContextWasBuilt();
    }

    @Test
    void tutorProvider_WhenTutorDoesNotExist_ShouldThrowException() {
        givenTutorDoesNotExist();

        whenResolvingTutorContextFails();

        thenTutorNotFoundExceptionIsThrown();
    }

    @Test
    void kiosqueroProvider_WhenBuffetExists_ShouldBuildContextAndUseUserOwner() {
        givenKiosqueroBuffetExists();
        givenKiosqueroContextData();

        whenResolvingKiosqueroContext();

        thenResolvedOwnerIsUser(userId);
        thenKiosqueroContextWasBuilt();
    }

    @Test
    void kiosqueroProvider_WhenBuffetDoesNotExist_ShouldThrowException() {
        givenKiosqueroBuffetDoesNotExist();

        whenResolvingKiosqueroContextFails();

        thenKiosqueroNotFoundExceptionIsThrown();
    }

    private StudentAssistantContextBuilder studentContextBuilder(
            SchoolAssistantRoleCatalog roleCatalog
    ) {
        return new StudentAssistantContextBuilder(
                walletRepository,
                paymentRepository,
                assistantPurchaseRepository,
                schoolCalendarRepository,
                roleCatalog,
                CLOCK
        );
    }

    private TutorAssistantContextBuilder tutorContextBuilder(
            SchoolAssistantRoleCatalog roleCatalog
    ) {
        return new TutorAssistantContextBuilder(
                paymentRepository,
                schoolCalendarRepository,
                purchaseRepository,
                presupuestoRepository,
                roleCatalog,
                CLOCK
        );
    }

    private KiosqueroAssistantContextBuilder kiosqueroContextBuilder(
            SchoolAssistantRoleCatalog roleCatalog
    ) {
        return new KiosqueroAssistantContextBuilder(
                productRepository,
                purchaseRepository,
                roleCatalog
        );
    }

    private void givenUserIdBelongsToStudent() {
        studentId = userId;
        when(alumnoRepository.existsById(userId)).thenReturn(true);
    }

    private void givenStudentCannotBeResolved() {
        when(alumnoRepository.existsById(userId)).thenReturn(false);
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.empty());
    }

    private void givenStudentContextData() {
        payment = payment();
        assistantPurchase = assistantPurchase();
        event = event();
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet()));
        when(paymentRepository.findRecentByStudentId(studentId, 5)).thenReturn(List.of(payment));
        when(assistantPurchaseRepository.findRecentByStudentId(studentId, 10))
                .thenReturn(List.of(assistantPurchase));
        when(schoolCalendarRepository.findByStudentIdAndDateRange(
                studentId,
                LocalDate.of(2026, 5, 27),
                LocalDate.of(2026, 6, 26)
        )).thenReturn(List.of(event));
    }

    private void givenTutorExists() {
        tutor = tutor();
        when(tutorRepository.findByUsuarioIdCognito(COGNITO_SUBJECT))
                .thenReturn(Optional.of(tutor));
    }

    private void givenTutorDoesNotExist() {
        when(tutorRepository.findByUsuarioIdCognito(COGNITO_SUBJECT))
                .thenReturn(Optional.empty());
    }

    private void givenTutorContextData() {
        purchase = purchase();
        when(purchaseRepository.findHistorialByTutor(tutorId)).thenReturn(List.of(purchase));
    }

    private void givenKiosqueroBuffetExists() {
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(buffetId);
    }

    private void givenKiosqueroBuffetDoesNotExist() {
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(null);
    }

    private void givenKiosqueroContextData() {
        product = product();
        purchase = purchase();
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(product));
        when(purchaseRepository.findByBuffetAndStatuses(eq(buffetId), any(), eq(10)))
                .thenReturn(List.of(purchase));
        when(purchaseRepository.findRecentByBuffet(buffetId, 10))
                .thenReturn(List.of(purchase));
    }

    private void whenResolvingStudentContext() {
        resolvedContext = studentProvider.resolve(user, COGNITO_SUBJECT);
    }

    private void whenResolvingStudentContextFails() {
        execution = () -> studentProvider.resolve(user, COGNITO_SUBJECT);
    }

    private void whenResolvingTutorContext() {
        resolvedContext = tutorProvider.resolve(user, COGNITO_SUBJECT);
    }

    private void whenResolvingTutorContextFails() {
        execution = () -> tutorProvider.resolve(user, COGNITO_SUBJECT);
    }

    private void whenResolvingKiosqueroContext() {
        resolvedContext = kiosqueroProvider.resolve(user, COGNITO_SUBJECT);
    }

    private void whenResolvingKiosqueroContextFails() {
        execution = () -> kiosqueroProvider.resolve(user, COGNITO_SUBJECT);
    }

    private void thenResolvedOwnerIsStudent(UUID expectedStudentId) {
        assertEquals(
                SchoolAssistantConversationOwnerType.STUDENT,
                resolvedContext.conversationOwner().type()
        );
        assertEquals(expectedStudentId, resolvedContext.conversationOwner().id());
    }

    private void thenResolvedOwnerIsUser(UUID expectedUserId) {
        assertEquals(
                SchoolAssistantConversationOwnerType.USER,
                resolvedContext.conversationOwner().type()
        );
        assertEquals(expectedUserId, resolvedContext.conversationOwner().id());
    }

    private void thenStudentContextWasBuilt() {
        SchoolAssistantContext context = resolvedContext.context();
        assertEquals(studentId, context.studentId());
        assertEquals(new BigDecimal("1500.00"), context.balance().orElseThrow());
        assertEquals(payment, context.recentPayments().get(0));
        assertEquals(assistantPurchase, context.recentPurchases().get(0));
        assertEquals(event, context.upcomingEvents().get(0));
        assertEquals(SchoolAssistantRole.ALUMNO, context.role());
    }

    private void thenStudentNotFoundExceptionIsThrown() {
        assertThrows(AlumnoNoEncontradoException.class, execution);
    }

    private void thenTutorContextWasBuilt() {
        SchoolAssistantContext context = resolvedContext.context();
        assertEquals(userId, context.userId());
        assertEquals(SchoolAssistantRole.TUTOR, context.role());
        assertEquals(1, context.recentPurchases().size());
        assertTrue(context.facts().contains("tutorId=" + tutorId));
        assertTrue(context.facts().stream().anyMatch(fact ->
                fact.startsWith("compraTutor=")));
    }

    private void thenTutorNotFoundExceptionIsThrown() {
        assertThrows(TutorNoEncontradoException.class, execution);
    }

    private void thenKiosqueroContextWasBuilt() {
        SchoolAssistantContext context = resolvedContext.context();
        assertEquals(userId, context.userId());
        assertEquals(buffetId, context.buffetId());
        assertEquals(SchoolAssistantRole.KIOSQUERO, context.role());
        assertEquals(1, context.recentPurchases().size());
        assertTrue(context.facts().contains("buffetId=" + buffetId));
        assertTrue(context.facts().contains("pedidosPendientes=1"));
        assertTrue(context.facts().stream().anyMatch(fact ->
                fact.contains("productoBuffet=")));
    }

    private void thenKiosqueroNotFoundExceptionIsThrown() {
        assertThrows(KiosqueroNotFoundException.class, execution);
    }

    private Usuario user(UUID userId) {
        Usuario user = new Usuario();
        user.setId(userId);
        return user;
    }

    private Tutor tutor() {
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        return tutor;
    }

    private Billetera wallet() {
        Billetera wallet = new Billetera();
        wallet.setSaldoCreditos(new BigDecimal("1500.00"));
        return wallet;
    }

    private SchoolAssistantPayment payment() {
        return new SchoolAssistantPayment(
                new BigDecimal("500.00"),
                EstadoPagoExterno.APROBADO,
                NOW.minusDays(1),
                NOW.minusDays(1)
        );
    }

    private SchoolAssistantPurchase assistantPurchase() {
        return new SchoolAssistantPurchase(
                new BigDecimal("320.00"),
                EstadoCompra.ENTREGADO,
                MedioPago.CREDITOS,
                NOW.minusHours(2)
        );
    }

    private SchoolCalendarEvent event() {
        return new SchoolCalendarEvent(
                LocalDate.of(2026, 5, 29),
                TipoDiaEscolar.JORNADA_ESPECIAL,
                "Acto escolar"
        );
    }

    private Purchase purchase() {
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setTotalAmount(new BigDecimal("900.00"));
        purchase.setStatus(EstadoCompra.PENDIENTE);
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setDate(NOW);
        return purchase;
    }

    private Producto product() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Alfajor");
        product.setPrecio(new BigDecimal("500.00"));
        product.setStockDisponible(20);
        product.setRequierePreparacion(false);
        return product;
    }
}
