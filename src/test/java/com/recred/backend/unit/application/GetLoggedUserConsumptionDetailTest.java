package com.recred.backend.unit.application;

import com.recred.backend.application.GetLoggedUserConsumptionDetail;
import com.recred.backend.application.GetLoggedUserConsumptionDetailRequest;
import com.recred.backend.application.GetLoggedUserConsumptionDetailResponse;
import com.recred.backend.domain.exceptions.AlumnoRequeridoParaConsumoException;
import com.recred.backend.domain.exceptions.ConsumoAlumnoNoAutorizadoException;
import com.recred.backend.domain.exceptions.RangoFechasConsumoInvalidoException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.ConsumptionRequester;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.StudentConsumptionProduct;
import com.recred.backend.domain.models.StudentConsumptionPurchase;
import com.recred.backend.domain.repositories.ConsumptionAccessRepository;
import com.recred.backend.domain.repositories.StudentConsumptionRepository;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetLoggedUserConsumptionDetailTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-28T12:00:00Z"),
            ZoneOffset.UTC
    );
    private static final String COGNITO_SUBJECT = "cognito-sub";

    @Mock
    private ConsumptionAccessRepository accessRepository;

    @Mock
    private StudentConsumptionRepository consumptionRepository;

    private GetLoggedUserConsumptionDetail useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetLoggedUserConsumptionDetail(
                accessRepository,
                consumptionRepository,
                CLOCK
        );
    }

    @Test
    void execute_WhenStudentOwnsConsumption_ShouldReturnDefaultPeriodDetail() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        StudentConsumptionPurchase purchase = purchase("1200.00");
        givenAuthenticatedRequester(new ConsumptionRequester(
                userId,
                RolUsuario.ALUMNO,
                studentId
        ));
        givenPurchasesFound(
                studentId,
                LocalDateTime.parse("2026-04-28T00:00:00"),
                LocalDateTime.parse("2026-05-28T23:59:59.999999999"),
                purchase
        );

        GetLoggedUserConsumptionDetailResponse response =
                whenGettingConsumption(null, null, null);

        assertEquals(studentId, response.studentId());
        assertEquals(LocalDate.of(2026, 4, 28), response.from());
        assertEquals(LocalDate.of(2026, 5, 28), response.to());
        assertEquals(new BigDecimal("1200.00"), response.totalAmount());
        assertEquals(1, response.purchaseCount());
        assertEquals("ENTREGADO", response.purchases().get(0).status());
        assertEquals("CREDITOS", response.purchases().get(0).paymentMethod());
        verify(consumptionRepository).findPurchasesByStudentIdAndDateRange(
                eq(studentId),
                eq(EstadoCompra.studentConsumptionStatuses()),
                any(),
                any()
        );
    }

    @Test
    void execute_WhenTutorCanAccessStudent_ShouldReturnRequestedPeriodDetail() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        givenAuthenticatedRequester(new ConsumptionRequester(
                userId,
                RolUsuario.PADRE,
                null
        ));
        givenTutorCanAccessStudent(userId, studentId, true);
        givenPurchasesFound(
                studentId,
                LocalDateTime.parse("2026-05-01T00:00:00"),
                LocalDateTime.parse("2026-05-20T23:59:59.999999999"),
                purchase("800.00"),
                purchase("200.00")
        );

        GetLoggedUserConsumptionDetailResponse response = whenGettingConsumption(
                studentId,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 20)
        );

        assertEquals(new BigDecimal("1000.00"), response.totalAmount());
        assertEquals(2, response.purchaseCount());
        verify(accessRepository).tutorCanAccessStudent(userId, studentId);
    }

    @Test
    void execute_WhenTutorDoesNotSendStudentId_ShouldThrowException() {
        UUID userId = UUID.randomUUID();
        givenAuthenticatedRequester(new ConsumptionRequester(
                userId,
                RolUsuario.PADRE,
                null
        ));

        assertThrows(
                AlumnoRequeridoParaConsumoException.class,
                () -> whenGettingConsumption(null, null, null)
        );
        thenConsumptionWasNotQueried();
    }

    @Test
    void execute_WhenTutorCannotAccessStudent_ShouldThrowException() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        givenAuthenticatedRequester(new ConsumptionRequester(
                userId,
                RolUsuario.PADRE,
                null
        ));
        givenTutorCanAccessStudent(userId, studentId, false);

        assertThrows(
                ConsumoAlumnoNoAutorizadoException.class,
                () -> whenGettingConsumption(studentId, null, null)
        );
        thenConsumptionWasNotQueried();
    }

    @Test
    void execute_WhenStudentSendsOwnStudentId_ShouldReturnConsumption() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        givenAuthenticatedRequester(new ConsumptionRequester(
                userId,
                RolUsuario.ALUMNO,
                studentId
        ));
        givenAnyPurchasesFound(studentId);

        GetLoggedUserConsumptionDetailResponse response = whenGettingConsumption(
                studentId,
                null,
                null
        );

        assertEquals(studentId, response.studentId());
    }

    @Test
    void execute_WhenStudentSendsAnotherStudentId_ShouldThrowException() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID anotherStudentId = UUID.randomUUID();
        givenAuthenticatedRequester(new ConsumptionRequester(
                userId,
                RolUsuario.ALUMNO,
                studentId
        ));

        assertThrows(
                ConsumoAlumnoNoAutorizadoException.class,
                () -> whenGettingConsumption(anotherStudentId, null, null)
        );
    }

    @Test
    void execute_WhenStudentHasNoLinkedStudent_ShouldThrowException() {
        UUID userId = UUID.randomUUID();
        givenAuthenticatedRequester(new ConsumptionRequester(
                userId,
                RolUsuario.ALUMNO,
                null
        ));

        assertThrows(
                ConsumoAlumnoNoAutorizadoException.class,
                () -> whenGettingConsumption(null, null, null)
        );
    }

    @Test
    void execute_WhenRequesterRoleCannotAccessConsumption_ShouldThrowException() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        givenAuthenticatedRequester(new ConsumptionRequester(
                userId,
                RolUsuario.ADMIN,
                null
        ));

        assertThrows(
                ConsumoAlumnoNoAutorizadoException.class,
                () -> whenGettingConsumption(studentId, null, null)
        );
    }

    @Test
    void execute_WhenDateRangeIsInvalid_ShouldThrowException() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        givenAuthenticatedRequester(new ConsumptionRequester(
                userId,
                RolUsuario.ALUMNO,
                studentId
        ));

        assertThrows(
                RangoFechasConsumoInvalidoException.class,
                () -> whenGettingConsumption(
                        null,
                        LocalDate.of(2026, 5, 20),
                        LocalDate.of(2026, 5, 1)
                )
        );
    }

    @Test
    void execute_WhenAuthenticatedUserDoesNotExist_ShouldThrowException() {
        givenAuthenticatedRequesterDoesNotExist();

        assertThrows(
                UsuarioAutenticadoNoEncontradoException.class,
                () -> whenGettingConsumption(null, null, null)
        );
    }

    @Test
    void execute_WhenRequestIsNull_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null));
    }

    private void givenAuthenticatedRequester(ConsumptionRequester requester) {
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(requester));
    }

    private void givenAuthenticatedRequesterDoesNotExist() {
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.empty());
    }

    private void givenTutorCanAccessStudent(
            UUID userId,
            UUID studentId,
            boolean canAccess
    ) {
        when(accessRepository.tutorCanAccessStudent(userId, studentId)).thenReturn(canAccess);
    }

    private void givenAnyPurchasesFound(UUID studentId) {
        when(consumptionRepository.findPurchasesByStudentIdAndDateRange(
                eq(studentId),
                anyCollection(),
                any(),
                any()
        )).thenReturn(List.of());
    }

    private void givenPurchasesFound(
            UUID studentId,
            LocalDateTime start,
            LocalDateTime end,
            StudentConsumptionPurchase... purchases
    ) {
        when(consumptionRepository.findPurchasesByStudentIdAndDateRange(
                eq(studentId),
                anyCollection(),
                eq(start),
                eq(end)
        )).thenReturn(List.of(purchases));
    }

    private GetLoggedUserConsumptionDetailResponse whenGettingConsumption(
            UUID studentId,
            LocalDate from,
            LocalDate to
    ) {
        return useCase.execute(new GetLoggedUserConsumptionDetailRequest(
                COGNITO_SUBJECT,
                studentId,
                from,
                to
        ));
    }

    private void thenConsumptionWasNotQueried() {
        verify(consumptionRepository, never()).findPurchasesByStudentIdAndDateRange(
                any(),
                anyCollection(),
                any(),
                any()
        );
    }

    private StudentConsumptionPurchase purchase(String totalAmount) {
        return new StudentConsumptionPurchase(
                UUID.randomUUID(),
                LocalDateTime.of(2026, 5, 20, 10, 0),
                EstadoCompra.ENTREGADO,
                MedioPago.CREDITOS,
                new BigDecimal(totalAmount),
                UUID.randomUUID(),
                "Buffet",
                List.of(new StudentConsumptionProduct(
                        UUID.randomUUID(),
                        "Sandwich",
                        "Comida",
                        1,
                        new BigDecimal(totalAmount),
                        new BigDecimal(totalAmount)
                ))
        );
    }
}
