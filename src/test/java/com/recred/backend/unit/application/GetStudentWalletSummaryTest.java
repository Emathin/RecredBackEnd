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
    void tutorCanSeeWalletSummaryForAssociatedStudent() {
        UUID userId = randomUUID();
        UUID studentId = randomUUID();
        Billetera wallet = wallet("1250.00");
        List<WalletMovement> movements = List.of(
                movement("3000.00", WalletMovementDirection.ENTRADA),
                movement("200.00", WalletMovementDirection.ENTRADA),
                movement("150.00", WalletMovementDirection.SALIDA)
        );
        List<StudentConsumptionPurchase> purchases = List.of(
                purchase("1000.00", product("Golosinas", "600.00", "No saludable"),
                        product("Golosinas", "400.00", "Poco saludable")),
                purchase("750.00", product("Bebidas", "450.00"),
                        product("Comida saludable", "300.00", "Saludable"))
        );
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 14);

        givenAuthenticatedTutorCanAccessStudent(userId, studentId);
        givenWalletExists(studentId, wallet);
        givenMovements(studentId, from, to, movements);
        givenPurchases(studentId, from, to, purchases);

        GetStudentWalletSummaryResponse summary =
                useCase.execute(requestWithDates(studentId, from, to));

        assertSummaryTotals(summary, studentId);
        assertCategorySpending(summary);
        assertHealthSpending(summary);

        verify(consumptionRepository).findPurchasesByStudentIdAndDateRange(
                eq(studentId),
                eq(Set.of(EstadoCompra.ENTREGADO)),
                any(),
                any()
        );
    }

    @Test
    void studentCanSeeOwnWalletSummaryWithDefaultPeriod() {
        UUID userId = randomUUID();
        UUID studentId = randomUUID();
        LocalDate defaultFrom = LocalDate.of(2026, 5, 16);
        LocalDate defaultTo = LocalDate.of(2026, 6, 15);

        givenAuthenticatedStudent(userId, studentId);
        givenWalletExists(studentId, wallet("10.00"));
        givenMovements(studentId, defaultFrom, defaultTo, List.of());
        givenPurchases(studentId, defaultFrom, defaultTo, List.of());

        GetStudentWalletSummaryResponse summary = useCase.execute(defaultRequest(studentId));

        assertEquals(LocalDate.of(2026, 5, 16), summary.from());
        assertEquals(LocalDate.of(2026, 6, 15), summary.to());
        assertEquals(BigDecimal.ZERO, summary.incomeAmount());
        assertEquals(BigDecimal.ZERO, summary.spentAmount());
    }

    @Test
    void tutorCannotSeeUnassociatedStudentWalletSummary() {
        UUID userId = randomUUID();
        UUID studentId = randomUUID();
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.PADRE,
                        null
                )));
        when(accessRepository.tutorCanAccessStudent(userId, studentId)).thenReturn(false);

        assertThrows(
                ConsumoAlumnoNoAutorizadoException.class,
                () -> useCase.execute(request(
                        COGNITO_SUBJECT,
                        studentId,
                        Optional.empty(),
                        Optional.empty()
                ))
        );
        verify(walletRepository, never()).findByStudentId(any());
    }

    @Test
    void rejectsInvalidDateRange() {
        UUID userId = randomUUID();
        UUID studentId = randomUUID();
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.ALUMNO,
                        studentId
                )));

        assertThrows(
                RangoFechasConsumoInvalidoException.class,
                () -> useCase.execute(request(
                        COGNITO_SUBJECT,
                        studentId,
                        Optional.of(LocalDate.of(2026, 6, 15)),
                        Optional.of(LocalDate.of(2026, 6, 1))
                ))
        );
    }

    @Test
    void rejectsMissingAuthenticatedUser() {
        UUID studentId = randomUUID();
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.empty());

        assertThrows(
                UsuarioAutenticadoNoEncontradoException.class,
                () -> useCase.execute(request(
                        COGNITO_SUBJECT,
                        studentId,
                        Optional.empty(),
                        Optional.empty()
                ))
        );
        verify(walletRepository, never()).findByStudentId(any());
    }

    @Test
    void rejectsStudentWithoutAssociatedStudentId() {
        UUID userId = randomUUID();
        UUID requestedStudentId = randomUUID();
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.ALUMNO,
                        null
                )));

        assertThrows(
                ConsumoAlumnoNoAutorizadoException.class,
                () -> useCase.execute(request(
                        COGNITO_SUBJECT,
                        requestedStudentId,
                        Optional.empty(),
                        Optional.empty()
                ))
        );
        verify(walletRepository, never()).findByStudentId(any());
    }

    @Test
    void rejectsWhenWalletDoesNotExistForStudent() {
        UUID userId = randomUUID();
        UUID studentId = randomUUID();
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.ALUMNO,
                        studentId
                )));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(request(
                        COGNITO_SUBJECT,
                        studentId,
                        Optional.empty(),
                        Optional.empty()
                ))
        );
    }

    @Test
    void groupsNullCategoryAsUncategorized() {
        UUID userId = randomUUID();
        UUID studentId = randomUUID();
        StudentConsumptionProduct product = product(null, "0.00");

        givenWalletSummaryWithPurchase(userId, studentId, product);

        GetStudentWalletSummaryResponse summary = useCase.execute(request(
                COGNITO_SUBJECT,
                studentId,
                Optional.empty(),
                Optional.empty()
        ));

        assertEquals("Sin categoria", summary.categorySpending().get(0).category());
    }

    @Test
    void groupsMissingHealthClassificationAsUnclassified() {
        UUID userId = randomUUID();
        UUID studentId = randomUUID();
        StudentConsumptionProduct product = product("Golosinas", "0.00");

        givenWalletSummaryWithPurchase(userId, studentId, product);

        GetStudentWalletSummaryResponse summary = useCase.execute(defaultRequest(studentId));

        assertEquals("Sin clasificar", summary.healthSpending().get(0).classification());
    }

    @Test
    void usesZeroPercentageWhenThereIsNoSpending() {
        UUID userId = randomUUID();
        UUID studentId = randomUUID();
        StudentConsumptionProduct product = product(null, "0.00");

        givenWalletSummaryWithPurchase(userId, studentId, product);

        GetStudentWalletSummaryResponse summary = useCase.execute(defaultRequest(studentId));

        assertEquals(new BigDecimal("0.00"), summary.categorySpending().get(0).percentage());
        assertEquals(new BigDecimal("0.00"), summary.healthSpending().get(0).percentage());
    }

    private Billetera wallet(String balance) {
        Billetera wallet = new Billetera();
        wallet.setId(randomUUID());
        wallet.setSaldoCreditos(new BigDecimal(balance));
        return wallet;
    }

    private GetStudentWalletSummaryRequest request(
            String cognitoSubject,
            UUID studentId,
            Optional<LocalDate> from,
            Optional<LocalDate> to
    ) {
        return new GetStudentWalletSummaryRequest(cognitoSubject, studentId, from, to);
    }

    private GetStudentWalletSummaryRequest defaultRequest(UUID studentId) {
        return request(COGNITO_SUBJECT, studentId, Optional.empty(), Optional.empty());
    }

    private GetStudentWalletSummaryRequest requestWithDates(
            UUID studentId,
            LocalDate from,
            LocalDate to) {
        return request(COGNITO_SUBJECT, studentId, Optional.of(from), Optional.of(to));
    }

    private void givenAuthenticatedTutorCanAccessStudent(UUID userId, UUID studentId) {
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.PADRE,
                        null
                )));
        when(accessRepository.tutorCanAccessStudent(userId, studentId)).thenReturn(true);
    }

    private void givenAuthenticatedStudent(UUID userId, UUID studentId) {
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.ALUMNO,
                        studentId
                )));
    }

    private void givenWalletExists(UUID studentId, Billetera wallet) {
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
    }

    private void givenWalletSummaryWithPurchase(
            UUID userId,
            UUID studentId,
            StudentConsumptionProduct product) {
        givenAuthenticatedStudent(userId, studentId);
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet("0.00")));
        when(movementRepository.findByStudentIdAndDateRange(eq(studentId), any(), any()))
                .thenReturn(List.of());
        when(consumptionRepository.findPurchasesByStudentIdAndDateRange(
                eq(studentId),
                anyCollection(),
                any(),
                any()
        )).thenReturn(List.of(purchase("0.00", product)));
    }

    private void givenMovements(
            UUID studentId,
            LocalDate from,
            LocalDate to,
            List<WalletMovement> movements) {
        when(movementRepository.findByStudentIdAndDateRange(
                eq(studentId),
                eq(from.atStartOfDay()),
                eq(to.atTime(23, 59, 59, 999999999))
        )).thenReturn(movements);
    }

    private void givenPurchases(
            UUID studentId,
            LocalDate from,
            LocalDate to,
            List<StudentConsumptionPurchase> purchases) {
        when(consumptionRepository.findPurchasesByStudentIdAndDateRange(
                eq(studentId),
                anyCollection(),
                eq(from.atStartOfDay()),
                eq(to.atTime(23, 59, 59, 999999999))
        )).thenReturn(purchases);
    }

    private void assertSummaryTotals(
            GetStudentWalletSummaryResponse summary,
            UUID studentId) {
        assertEquals(studentId, summary.studentId());
        assertEquals(new BigDecimal("1250.00"), summary.currentBalance());
        assertEquals(new BigDecimal("3200.00"), summary.incomeAmount());
        assertEquals(new BigDecimal("1750.00"), summary.spentAmount());
        assertEquals(new BigDecimal("1450.00"), summary.periodBalance());
        assertEquals(2, summary.purchaseCount());
        assertEquals(3, summary.movements().size());
    }

    private void assertCategorySpending(GetStudentWalletSummaryResponse summary) {
        assertEquals("Golosinas", summary.categorySpending().get(0).category());
        assertEquals(new BigDecimal("1000.00"), summary.categorySpending().get(0).amount());
        assertEquals(new BigDecimal("57.14"), summary.categorySpending().get(0).percentage());
    }

    private void assertHealthSpending(GetStudentWalletSummaryResponse summary) {
        assertEquals("No saludable", summary.healthSpending().get(0).classification());
        assertEquals(new BigDecimal("600.00"), summary.healthSpending().get(0).amount());
        assertEquals(new BigDecimal("34.29"), summary.healthSpending().get(0).percentage());
        assertEquals("Sin clasificar", summary.healthSpending().get(1).classification());
        assertEquals(new BigDecimal("450.00"), summary.healthSpending().get(1).amount());
        assertEquals("Poco saludable", summary.healthSpending().get(2).classification());
        assertEquals(new BigDecimal("400.00"), summary.healthSpending().get(2).amount());
        assertEquals("Saludable", summary.healthSpending().get(3).classification());
        assertEquals(new BigDecimal("300.00"), summary.healthSpending().get(3).amount());
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
