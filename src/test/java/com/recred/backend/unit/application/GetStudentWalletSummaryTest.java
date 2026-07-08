package com.recred.backend.unit.application;

import com.recred.backend.application.GetStudentWalletSummary;
import com.recred.backend.application.GetStudentWalletSummaryRequest;
import com.recred.backend.application.GetStudentWalletSummaryResponse;
import com.recred.backend.domain.exceptions.ConsumoAlumnoNoAutorizadoException;
import com.recred.backend.domain.exceptions.RangoFechasConsumoInvalidoException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.ConsumptionRequester;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.StudentConsumptionProduct;
import com.recred.backend.domain.models.StudentConsumptionPurchase;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.models.WalletMovement;
import com.recred.backend.domain.models.WalletMovementDirection;
import com.recred.backend.domain.repositories.ConsumptionAccessRepository;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.StudentConsumptionRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStudentWalletSummaryTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-15T12:00:00Z"),
            ZoneOffset.UTC
    );
    private static final String COGNITO_SUBJECT = "cognito-sub";

    @Mock
    private ConsumptionAccessRepository accessRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private MovimientoBilleteraRepository movementRepository;

    @Mock
    private StudentConsumptionRepository consumptionRepository;

    private GetStudentWalletSummary useCase;

    private UUID userId;
    private UUID studentId;
    private LocalDate from;
    private LocalDate to;
    private GetStudentWalletSummaryResponse response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new GetStudentWalletSummary(
                accessRepository,
                walletRepository,
                movementRepository,
                consumptionRepository,
                CLOCK
        );
    }

    @Test
    void givenTutorCanAccessStudent_whenGettingWalletSummary_thenReturnsWalletSummary() {
        givenWalletSummaryScenario();
        givenExplicitPeriod();
        givenAuthenticatedTutorCanAccessStudent();
        givenStudentWalletExists("1250.00");
        givenStudentHasMovements(List.of(
                movement("3000.00", WalletMovementDirection.ENTRADA),
                movement("200.00", WalletMovementDirection.ENTRADA),
                movement("150.00", WalletMovementDirection.SALIDA)
        ));
        givenStudentHasPurchases(List.of(
                purchase("1000.00", product("Golosinas", "600.00", "No saludable"),
                        product("Golosinas", "400.00", "Poco saludable")),
                purchase("750.00", product("Bebidas", "450.00"),
                        product("Comida saludable", "300.00", "Saludable"))
        ));

        whenGettingWalletSummary();

        thenWalletSummaryTotalsAreReturned();
        thenCategorySpendingIsReturned();
        thenHealthSpendingIsReturned();
        thenConsumptionWasRequestedForDeliveredPurchases();
    }

    @Test
    void givenStudentRequester_whenGettingWalletSummaryWithoutDates_thenUsesDefaultPeriod() {
        givenWalletSummaryScenario();
        givenDefaultPeriod();
        givenAuthenticatedStudent();
        givenStudentWalletExists("10.00");
        givenStudentHasMovements(List.of());
        givenStudentHasPurchases(List.of());

        whenGettingDefaultWalletSummary();

        thenDefaultPeriodIsReturned();
        thenEmptyAmountsAreReturned();
    }

    @Test
    void givenTutorCannotAccessStudent_whenGettingWalletSummary_thenFails() {
        givenWalletSummaryScenario();
        givenAuthenticatedTutorCannotAccessStudent();

        whenGettingDefaultWalletSummaryFails();

        thenStudentConsumptionIsNotAuthorized();
        thenWalletWasNotRequested();
    }

    @Test
    void givenInvalidDateRange_whenGettingWalletSummary_thenFails() {
        givenWalletSummaryScenario();
        givenInvalidPeriod();
        givenAuthenticatedStudent();

        whenGettingWalletSummaryFails();

        thenDateRangeIsInvalid();
    }

    @Test
    void givenMissingAuthenticatedUser_whenGettingWalletSummary_thenFails() {
        givenWalletSummaryScenario();
        givenAuthenticatedUserDoesNotExist();

        whenGettingDefaultWalletSummaryFails();

        thenAuthenticatedUserWasNotFound();
        thenWalletWasNotRequested();
    }

    @Test
    void givenStudentRequesterWithoutStudentId_whenGettingWalletSummary_thenFails() {
        givenWalletSummaryScenario();
        givenAuthenticatedStudentWithoutAssociatedStudentId();

        whenGettingDefaultWalletSummaryFails();

        thenStudentConsumptionIsNotAuthorized();
        thenWalletWasNotRequested();
    }

    @Test
    void givenStudentWithoutWallet_whenGettingWalletSummary_thenFails() {
        givenWalletSummaryScenario();
        givenAuthenticatedStudent();
        givenStudentWalletDoesNotExist();

        whenGettingDefaultWalletSummaryFails();

        thenWalletNotFoundIsThrown();
    }

    @Test
    void givenPurchaseWithoutCategory_whenGettingWalletSummary_thenGroupsAsUncategorized() {
        givenWalletSummaryScenario();
        givenDefaultPeriod();
        givenWalletSummaryWithPurchase(product(null, "0.00"));

        whenGettingDefaultWalletSummary();

        thenPurchaseIsGroupedWithoutCategory();
    }

    @Test
    void givenProductWithoutHealthClassification_whenGettingSummary_thenGroupsAsUnclassified() {
        givenWalletSummaryScenario();
        givenDefaultPeriod();
        givenWalletSummaryWithPurchase(product("Golosinas", "0.00"));

        whenGettingDefaultWalletSummary();

        thenPurchaseIsGroupedWithoutHealthClassification();
    }

    @Test
    void givenPurchaseWithoutSpending_whenGettingWalletSummary_thenUsesZeroPercentages() {
        givenWalletSummaryScenario();
        givenDefaultPeriod();
        givenWalletSummaryWithPurchase(product(null, "0.00"));

        whenGettingDefaultWalletSummary();

        thenZeroPercentagesAreReturned();
    }

    private void givenWalletSummaryScenario() {
        userId = randomUUID();
        studentId = randomUUID();
    }

    private void givenExplicitPeriod() {
        from = LocalDate.of(2026, 6, 1);
        to = LocalDate.of(2026, 6, 14);
    }

    private void givenDefaultPeriod() {
        from = LocalDate.of(2026, 5, 16);
        to = LocalDate.of(2026, 6, 15);
    }

    private void givenInvalidPeriod() {
        from = LocalDate.of(2026, 6, 15);
        to = LocalDate.of(2026, 6, 1);
    }

    private void givenAuthenticatedTutorCanAccessStudent() {
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.PADRE,
                        null
                )));
        when(accessRepository.tutorCanAccessStudent(userId, studentId)).thenReturn(true);
    }

    private void givenAuthenticatedTutorCannotAccessStudent() {
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.PADRE,
                        null
                )));
        when(accessRepository.tutorCanAccessStudent(userId, studentId)).thenReturn(false);
    }

    private void givenAuthenticatedStudent() {
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.ALUMNO,
                        studentId
                )));
    }

    private void givenAuthenticatedStudentWithoutAssociatedStudentId() {
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.ALUMNO,
                        null
                )));
    }

    private void givenAuthenticatedUserDoesNotExist() {
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.empty());
    }

    private void givenStudentWalletExists(String balance) {
        when(walletRepository.findByStudentId(studentId))
                .thenReturn(Optional.of(wallet(balance)));
    }

    private void givenStudentWalletDoesNotExist() {
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.empty());
    }

    private void givenStudentHasMovements(List<WalletMovement> movements) {
        when(movementRepository.findByStudentIdAndDateRange(
                eq(studentId),
                eq(from.atStartOfDay()),
                eq(to.atTime(23, 59, 59, 999999999))
        )).thenReturn(movements);
    }

    private void givenStudentHasPurchases(List<StudentConsumptionPurchase> purchases) {
        when(consumptionRepository.findPurchasesByStudentIdAndDateRange(
                eq(studentId),
                anyCollection(),
                eq(from.atStartOfDay()),
                eq(to.atTime(23, 59, 59, 999999999))
        )).thenReturn(purchases);
    }

    private void givenWalletSummaryWithPurchase(StudentConsumptionProduct product) {
        givenAuthenticatedStudent();
        givenStudentWalletExists("0.00");
        givenStudentHasMovements(List.of());
        givenStudentHasPurchases(List.of(purchase("0.00", product)));
    }

    private void whenGettingWalletSummary() {
        response = useCase.execute(requestWithDates(studentId, from, to));
    }

    private void whenGettingDefaultWalletSummary() {
        response = useCase.execute(defaultRequest(studentId));
    }

    private void whenGettingWalletSummaryFails() {
        thrownException = assertThrows(RuntimeException.class, this::whenGettingWalletSummary);
    }

    private void whenGettingDefaultWalletSummaryFails() {
        thrownException = assertThrows(
                RuntimeException.class,
                this::whenGettingDefaultWalletSummary);
    }

    private void thenWalletSummaryTotalsAreReturned() {
        assertEquals(studentId, response.studentId());
        assertEquals(new BigDecimal("1250.00"), response.currentBalance());
        assertEquals(new BigDecimal("3200.00"), response.incomeAmount());
        assertEquals(new BigDecimal("1750.00"), response.spentAmount());
        assertEquals(new BigDecimal("1450.00"), response.periodBalance());
        assertEquals(2, response.purchaseCount());
        assertEquals(3, response.movements().size());
    }

    private void thenCategorySpendingIsReturned() {
        assertEquals("Golosinas", response.categorySpending().get(0).category());
        assertEquals(new BigDecimal("1000.00"), response.categorySpending().get(0).amount());
        assertEquals(new BigDecimal("57.14"), response.categorySpending().get(0).percentage());
    }

    private void thenHealthSpendingIsReturned() {
        assertEquals("No saludable", response.healthSpending().get(0).classification());
        assertEquals(new BigDecimal("600.00"), response.healthSpending().get(0).amount());
        assertEquals(new BigDecimal("34.29"), response.healthSpending().get(0).percentage());
        assertEquals("Sin clasificar", response.healthSpending().get(1).classification());
        assertEquals(new BigDecimal("450.00"), response.healthSpending().get(1).amount());
        assertEquals("Poco saludable", response.healthSpending().get(2).classification());
        assertEquals(new BigDecimal("400.00"), response.healthSpending().get(2).amount());
        assertEquals("Saludable", response.healthSpending().get(3).classification());
        assertEquals(new BigDecimal("300.00"), response.healthSpending().get(3).amount());
    }

    private void thenConsumptionWasRequestedForDeliveredPurchases() {
        verify(consumptionRepository).findPurchasesByStudentIdAndDateRange(
                eq(studentId),
                eq(Set.of(EstadoCompra.ENTREGADO)),
                any(),
                any()
        );
    }

    private void thenDefaultPeriodIsReturned() {
        assertEquals(LocalDate.of(2026, 5, 16), response.from());
        assertEquals(LocalDate.of(2026, 6, 15), response.to());
    }

    private void thenEmptyAmountsAreReturned() {
        assertEquals(BigDecimal.ZERO, response.incomeAmount());
        assertEquals(BigDecimal.ZERO, response.spentAmount());
    }

    private void thenStudentConsumptionIsNotAuthorized() {
        assertTrue(thrownException instanceof ConsumoAlumnoNoAutorizadoException);
    }

    private void thenDateRangeIsInvalid() {
        assertTrue(thrownException instanceof RangoFechasConsumoInvalidoException);
    }

    private void thenAuthenticatedUserWasNotFound() {
        assertTrue(thrownException instanceof UsuarioAutenticadoNoEncontradoException);
    }

    private void thenWalletNotFoundIsThrown() {
        assertTrue(thrownException instanceof IllegalArgumentException);
    }

    private void thenWalletWasNotRequested() {
        verify(walletRepository, never()).findByStudentId(any());
    }

    private void thenPurchaseIsGroupedWithoutCategory() {
        assertEquals("Sin categoria", response.categorySpending().get(0).category());
    }

    private void thenPurchaseIsGroupedWithoutHealthClassification() {
        assertEquals("Sin clasificar", response.healthSpending().get(0).classification());
    }

    private void thenZeroPercentagesAreReturned() {
        assertEquals(new BigDecimal("0.00"), response.categorySpending().get(0).percentage());
        assertEquals(new BigDecimal("0.00"), response.healthSpending().get(0).percentage());
    }

    private Billetera wallet(String balance) {
        Billetera wallet = new Billetera();
        wallet.setId(randomUUID());
        wallet.setSaldoCreditos(new BigDecimal(balance));
        return wallet;
    }

    private GetStudentWalletSummaryRequest defaultRequest(UUID studentId) {
        return new GetStudentWalletSummaryRequest(
                COGNITO_SUBJECT,
                studentId,
                Optional.empty(),
                Optional.empty());
    }

    private GetStudentWalletSummaryRequest requestWithDates(
            UUID studentId,
            LocalDate from,
            LocalDate to) {
        return new GetStudentWalletSummaryRequest(
                COGNITO_SUBJECT,
                studentId,
                Optional.of(from),
                Optional.of(to));
    }

    private WalletMovement movement(String amount, WalletMovementDirection direction) {
        return new WalletMovement(
                randomUUID(),
                LocalDateTime.of(2026, 6, 14, 10, 0),
                TipoMovimientoBilletera.CARGA,
                "Movimiento",
                new BigDecimal(amount),
                direction
        );
    }

    private StudentConsumptionPurchase purchase(
            String totalAmount,
            StudentConsumptionProduct... products) {
        return new StudentConsumptionPurchase(
                randomUUID(),
                LocalDateTime.of(2026, 6, 14, 10, 0),
                EstadoCompra.ENTREGADO,
                MedioPago.CREDITOS,
                new BigDecimal(totalAmount),
                randomUUID(),
                "Buffet",
                List.of(products)
        );
    }

    private StudentConsumptionProduct product(String category, String subtotal) {
        return new StudentConsumptionProduct(
                randomUUID(),
                "Producto",
                category,
                1,
                new BigDecimal(subtotal),
                new BigDecimal(subtotal)
        );
    }

    private StudentConsumptionProduct product(
            String category,
            String subtotal,
            String healthClassification) {
        return new StudentConsumptionProduct(
                randomUUID(),
                "Producto",
                category,
                1,
                new BigDecimal(subtotal),
                new BigDecimal(subtotal),
                List.of(healthClassification)
        );
    }
}
