package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoRequeridoParaConsumoException;
import com.recred.backend.domain.exceptions.ConsumoAlumnoNoAutorizadoException;
import com.recred.backend.domain.exceptions.RangoFechasConsumoInvalidoException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.ConsumptionRequester;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.StudentConsumptionDetail;
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
import java.util.Set;
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
    void studentUserCanSeeOwnConsumptionWithoutSendingStudentId() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        StudentConsumptionPurchase purchase = purchase("1200.00");
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.ALUMNO,
                        studentId
                )));
        when(consumptionRepository.findPurchasesByStudentIdAndDateRange(
                eq(studentId),
                anyCollection(),
                eq(LocalDateTime.parse("2026-04-28T00:00:00")),
                eq(LocalDateTime.parse("2026-05-28T23:59:59.999999999"))
        )).thenReturn(List.of(purchase));

        StudentConsumptionDetail detail = useCase.execute(
                COGNITO_SUBJECT,
                new StudentConsumptionCommand(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                )
        );

        assertEquals(studentId, detail.studentId());
        assertEquals(LocalDate.of(2026, 4, 28), detail.from());
        assertEquals(LocalDate.of(2026, 5, 28), detail.to());
        assertEquals(new BigDecimal("1200.00"), detail.totalAmount());
        assertEquals(1, detail.purchaseCount());

        verify(consumptionRepository).findPurchasesByStudentIdAndDateRange(
                eq(studentId),
                eq(Set.of(EstadoCompra.ENTREGADO)),
                any(),
                any()
        );
    }

    @Test
    void tutorUserCanSeeAssociatedStudentConsumption() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.PADRE,
                        null
                )));
        when(accessRepository.tutorCanAccessStudent(userId, studentId)).thenReturn(true);
        when(consumptionRepository.findPurchasesByStudentIdAndDateRange(
                eq(studentId),
                anyCollection(),
                eq(LocalDateTime.parse("2026-05-01T00:00:00")),
                eq(LocalDateTime.parse("2026-05-20T23:59:59.999999999"))
        )).thenReturn(List.of(purchase("800.00"), purchase("200.00")));

        StudentConsumptionDetail detail = useCase.execute(
                COGNITO_SUBJECT,
                new StudentConsumptionCommand(
                        Optional.of(studentId),
                        Optional.of(LocalDate.of(2026, 5, 1)),
                        Optional.of(LocalDate.of(2026, 5, 20))
                )
        );

        assertEquals(new BigDecimal("1000.00"), detail.totalAmount());
        assertEquals(2, detail.purchaseCount());
        verify(accessRepository).tutorCanAccessStudent(userId, studentId);
    }

    @Test
    void tutorUserMustSendStudentId() {
        UUID userId = UUID.randomUUID();
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.PADRE,
                        null
                )));

        assertThrows(
                AlumnoRequeridoParaConsumoException.class,
                () -> useCase.execute(
                        COGNITO_SUBJECT,
                        new StudentConsumptionCommand(
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()
                        )
                )
        );
        verify(consumptionRepository, never()).findPurchasesByStudentIdAndDateRange(
                any(),
                anyCollection(),
                any(),
                any()
        );
    }

    @Test
    void tutorUserCannotSeeUnassociatedStudentConsumption() {
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
                        new StudentConsumptionCommand(
                                Optional.of(studentId),
                                Optional.empty(),
                                Optional.empty()
                        )
                )
        );
        verify(consumptionRepository, never()).findPurchasesByStudentIdAndDateRange(
                any(),
                anyCollection(),
                any(),
                any()
        );
    }

    @Test
    void studentUserCanSendOwnStudentId() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.ALUMNO,
                        studentId
                )));
        when(consumptionRepository.findPurchasesByStudentIdAndDateRange(
                eq(studentId),
                anyCollection(),
                any(),
                any()
        )).thenReturn(List.of());

        StudentConsumptionDetail detail = useCase.execute(
                COGNITO_SUBJECT,
                new StudentConsumptionCommand(
                        Optional.of(studentId),
                        Optional.empty(),
                        Optional.empty()
                )
        );

        assertEquals(studentId, detail.studentId());
    }

    @Test
    void studentUserCannotSendAnotherStudentId() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID anotherStudentId = UUID.randomUUID();
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.ALUMNO,
                        studentId
                )));

        assertThrows(
                ConsumoAlumnoNoAutorizadoException.class,
                () -> useCase.execute(
                        COGNITO_SUBJECT,
                        new StudentConsumptionCommand(
                                Optional.of(anotherStudentId),
                                Optional.empty(),
                                Optional.empty()
                        )
                )
        );
    }

    @Test
    void studentUserWithoutLinkedStudentCannotSeeConsumption() {
        UUID userId = UUID.randomUUID();
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
                        new StudentConsumptionCommand(
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()
                        )
                )
        );
    }

    @Test
    void adminUserCannotSeeConsumption() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.ADMIN,
                        null
                )));

        assertThrows(
                ConsumoAlumnoNoAutorizadoException.class,
                () -> useCase.execute(
                        COGNITO_SUBJECT,
                        new StudentConsumptionCommand(
                                Optional.of(studentId),
                                Optional.empty(),
                                Optional.empty()
                        )
                )
        );
    }

    @Test
    void rejectInvalidDateRange() {
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
                        new StudentConsumptionCommand(
                                Optional.empty(),
                                Optional.of(LocalDate.of(2026, 5, 20)),
                                Optional.of(LocalDate.of(2026, 5, 1))
                        )
                )
        );
    }

    @Test
    void rejectUnknownAuthenticatedUser() {
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.empty());

        assertThrows(
                UsuarioAutenticadoNoEncontradoException.class,
                () -> useCase.execute(
                        COGNITO_SUBJECT,
                        new StudentConsumptionCommand(
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()
                        )
                )
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
