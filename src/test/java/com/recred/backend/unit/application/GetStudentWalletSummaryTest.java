package com.recred.backend.application;

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
import com.recred.backend.domain.models.StudentWalletSummary;
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
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
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
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.PADRE,
                        null
                )));
        when(accessRepository.tutorCanAccessStudent(userId, studentId)).thenReturn(true);
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
        when(movementRepository.findByStudentIdAndDateRange(
                eq(studentId),
                eq(LocalDateTime.parse("2026-06-01T00:00:00")),
                eq(LocalDateTime.parse("2026-06-14T23:59:59.999999999"))
        )).thenReturn(movements);
        when(consumptionRepository.findPurchasesByStudentIdAndDateRange(
                eq(studentId),
                anyCollection(),
                eq(LocalDateTime.parse("2026-06-01T00:00:00")),
                eq(LocalDateTime.parse("2026-06-14T23:59:59.999999999"))
        )).thenReturn(purchases);

        StudentWalletSummary summary = useCase.execute(
                COGNITO_SUBJECT,
                studentId,
                Optional.of(LocalDate.of(2026, 6, 1)),
                Optional.of(LocalDate.of(2026, 6, 14))
        );

        assertEquals(studentId, summary.studentId());
        assertEquals(new BigDecimal("1250.00"), summary.currentBalance());
        assertEquals(new BigDecimal("3200.00"), summary.incomeAmount());
        assertEquals(new BigDecimal("1750.00"), summary.spentAmount());
        assertEquals(new BigDecimal("1450.00"), summary.periodBalance());
        assertEquals(2, summary.purchaseCount());
        assertEquals(3, summary.movements().size());
        assertEquals("Golosinas", summary.categorySpending().get(0).category());
        assertEquals(new BigDecimal("1000.00"), summary.categorySpending().get(0).amount());
        assertEquals(new BigDecimal("57.14"), summary.categorySpending().get(0).percentage());
        assertEquals("No saludable", summary.healthSpending().get(0).classification());
        assertEquals(new BigDecimal("600.00"), summary.healthSpending().get(0).amount());
        assertEquals(new BigDecimal("34.29"), summary.healthSpending().get(0).percentage());
        assertEquals("Sin clasificar", summary.healthSpending().get(1).classification());
        assertEquals(new BigDecimal("450.00"), summary.healthSpending().get(1).amount());
        assertEquals("Poco saludable", summary.healthSpending().get(2).classification());
        assertEquals(new BigDecimal("400.00"), summary.healthSpending().get(2).amount());
        assertEquals("Saludable", summary.healthSpending().get(3).classification());
        assertEquals(new BigDecimal("300.00"), summary.healthSpending().get(3).amount());

        verify(consumptionRepository).findPurchasesByStudentIdAndDateRange(
                eq(studentId),
                eq(Set.of(EstadoCompra.ENTREGADO)),
                any(),
                any()
        );
    }

    @Test
    void studentCanSeeOwnWalletSummaryWithDefaultPeriod() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.ALUMNO,
                        studentId
                )));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet("10.00")));
        when(movementRepository.findByStudentIdAndDateRange(
                eq(studentId),
                eq(LocalDateTime.parse("2026-05-16T00:00:00")),
                eq(LocalDateTime.parse("2026-06-15T23:59:59.999999999"))
        )).thenReturn(List.of());
        when(consumptionRepository.findPurchasesByStudentIdAndDateRange(
                eq(studentId),
                anyCollection(),
                eq(LocalDateTime.parse("2026-05-16T00:00:00")),
                eq(LocalDateTime.parse("2026-06-15T23:59:59.999999999"))
        )).thenReturn(List.of());

        StudentWalletSummary summary = useCase.execute(
                COGNITO_SUBJECT,
                studentId,
                Optional.empty(),
                Optional.empty()
        );

        assertEquals(LocalDate.of(2026, 5, 16), summary.from());
        assertEquals(LocalDate.of(2026, 6, 15), summary.to());
        assertEquals(BigDecimal.ZERO, summary.incomeAmount());
        assertEquals(BigDecimal.ZERO, summary.spentAmount());
    }

    @Test
    void tutorCannotSeeUnassociatedStudentWalletSummary() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.PADRE,
                        null
                )));
        when(accessRepository.tutorCanAccessStudent(userId, studentId)).thenReturn(false);

        assertThrows(
                ConsumoAlumnoNoAutorizadoException.class,
                () -> useCase.execute(
                        COGNITO_SUBJECT,
                        studentId,
                        Optional.empty(),
                        Optional.empty()
                )
        );
        verify(walletRepository, never()).findByStudentId(any());
    }

    @Test
    void rejectsInvalidDateRange() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.ALUMNO,
                        studentId
                )));

        assertThrows(
                RangoFechasConsumoInvalidoException.class,
                () -> useCase.execute(
                        COGNITO_SUBJECT,
                        studentId,
                        Optional.of(LocalDate.of(2026, 6, 15)),
                        Optional.of(LocalDate.of(2026, 6, 1))
                )
        );
    }

    @Test
    void rejectsMissingAuthenticatedUser() {
        UUID studentId = UUID.randomUUID();
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.empty());

        assertThrows(
                UsuarioAutenticadoNoEncontradoException.class,
                () -> useCase.execute(
                        COGNITO_SUBJECT,
                        studentId,
                        Optional.empty(),
                        Optional.empty()
                )
        );
        verify(walletRepository, never()).findByStudentId(any());
    }

    @Test
    void rejectsStudentWithoutAssociatedStudentId() {
        UUID userId = UUID.randomUUID();
        UUID requestedStudentId = UUID.randomUUID();
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.ALUMNO,
                        null
                )));

        assertThrows(
                ConsumoAlumnoNoAutorizadoException.class,
                () -> useCase.execute(
                        COGNITO_SUBJECT,
                        requestedStudentId,
                        Optional.empty(),
                        Optional.empty()
                )
        );
        verify(walletRepository, never()).findByStudentId(any());
    }

    @Test
    void rejectsWhenWalletDoesNotExistForStudent() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.ALUMNO,
                        studentId
                )));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(
                        COGNITO_SUBJECT,
                        studentId,
                        Optional.empty(),
                        Optional.empty()
                )
        );
    }

    @Test
    void groupsNullCategoryAsUncategorizedAndUsesZeroPercentageWhenNoSpending() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        StudentConsumptionProduct product = product(null, "0.00");
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.ALUMNO,
                        studentId
                )));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet("0.00")));
        when(movementRepository.findByStudentIdAndDateRange(eq(studentId), any(), any()))
                .thenReturn(List.of());
        when(consumptionRepository.findPurchasesByStudentIdAndDateRange(
                eq(studentId),
                anyCollection(),
                any(),
                any()
        )).thenReturn(List.of(purchase("0.00", product)));

        StudentWalletSummary summary = useCase.execute(
                COGNITO_SUBJECT,
                studentId,
                Optional.empty(),
                Optional.empty()
        );

        assertEquals("Sin categoria", summary.categorySpending().get(0).category());
        assertEquals(new BigDecimal("0.00"), summary.categorySpending().get(0).percentage());
        assertEquals("Sin clasificar", summary.healthSpending().get(0).classification());
        assertEquals(new BigDecimal("0.00"), summary.healthSpending().get(0).percentage());
    }

    private Billetera wallet(String balance) {
        Billetera wallet = new Billetera();
        wallet.setId(UUID.randomUUID());
        wallet.setSaldoCreditos(new BigDecimal(balance));
        return wallet;
    }

    private WalletMovement movement(String amount, WalletMovementDirection direction) {
        return new WalletMovement(
                UUID.randomUUID(),
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
                UUID.randomUUID(),
                LocalDateTime.of(2026, 6, 14, 10, 0),
                EstadoCompra.ENTREGADO,
                MedioPago.CREDITOS,
                new BigDecimal(totalAmount),
                UUID.randomUUID(),
                "Buffet",
                List.of(products)
        );
    }

    private StudentConsumptionProduct product(String category, String subtotal) {
        return new StudentConsumptionProduct(
                UUID.randomUUID(),
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
                UUID.randomUUID(),
                "Producto",
                category,
                1,
                new BigDecimal(subtotal),
                new BigDecimal(subtotal),
                List.of(healthClassification)
        );
    }
}
