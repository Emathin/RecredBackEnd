package com.recred.backend.unit.application;

import com.recred.backend.application.GetPurchaseStatus;
import com.recred.backend.application.GetPurchaseStatusRequest;
import com.recred.backend.application.GetPurchaseStatusResponse;
import com.recred.backend.domain.exceptions.PurchaseAccessDeniedException;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ConsumptionRequester;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.repositories.ConsumptionAccessRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPurchaseStatusTest {

    private static final String COGNITO_SUBJECT = "cognito-subject";

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private ConsumptionAccessRepository accessRepository;

    @InjectMocks
    private GetPurchaseStatus useCase;

    private UUID purchaseId;
    private UUID studentId;
    private UUID userId;
    private Purchase purchase;

    @BeforeEach
    void setUp() {
        purchaseId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        userId = UUID.randomUUID();

        Alumno student = new Alumno();
        student.setId(studentId);

        purchase = new Purchase();
        purchase.setId(purchaseId);
        purchase.setStudent(student);
        purchase.setStatus(EstadoCompra.PENDIENTE);
    }

    @Test
    void execute_WhenStudentOwnsPurchase_ShouldReturnStatus() {
        ConsumptionRequester requester = new ConsumptionRequester(
                userId,
                RolUsuario.ALUMNO,
                studentId
        );

        givenPurchaseExists();
        givenAuthenticatedRequester(requester);
        GetPurchaseStatusResponse result = whenGettingPurchaseStatus();

        assertEquals(purchaseId, result.purchaseId());
        assertEquals(studentId, result.studentId());
        assertEquals("PENDIENTE", result.status());
        assertEquals("A preparar", result.statusLabel());
        verify(accessRepository, never()).tutorCanAccessStudent(userId, studentId);
    }

    @Test
    void execute_WhenTutorCanAccessStudent_ShouldReturnStatus() {
        ConsumptionRequester requester = new ConsumptionRequester(
                userId,
                RolUsuario.PADRE,
                null
        );

        givenPurchaseExists();
        givenAuthenticatedRequester(requester);
        givenTutorCanAccessStudent(true);
        GetPurchaseStatusResponse result = whenGettingPurchaseStatus();

        assertEquals("PENDIENTE", result.status());
        verify(accessRepository).tutorCanAccessStudent(userId, studentId);
    }

    @Test
    void execute_WhenAuthenticatedUserDoesNotExist_ShouldThrowException() {
        givenPurchaseExists();
        givenAuthenticatedRequesterDoesNotExist();

        assertThrows(UsuarioAutenticadoNoEncontradoException.class,
                this::whenGettingPurchaseStatus);
    }

    @Test
    void execute_WhenRequesterCannotAccessPurchase_ShouldThrowException() {
        ConsumptionRequester requester = new ConsumptionRequester(
                userId,
                RolUsuario.ALUMNO,
                UUID.randomUUID()
        );
        givenPurchaseExists();
        givenAuthenticatedRequester(requester);

        assertThrows(PurchaseAccessDeniedException.class,
                this::whenGettingPurchaseStatus);
    }

    @Test
    void execute_WhenPurchaseHasNoStudent_ShouldThrowException() {
        purchase.setStudent(null);
        ConsumptionRequester requester = new ConsumptionRequester(
                userId,
                RolUsuario.ALUMNO,
                studentId
        );
        givenPurchaseExists();
        givenAuthenticatedRequester(requester);

        assertThrows(PurchaseAccessDeniedException.class,
                this::whenGettingPurchaseStatus);
    }

    @Test
    void execute_WhenPurchaseStudentHasNoId_ShouldThrowException() {
        purchase.getStudent().setId(null);
        ConsumptionRequester requester = new ConsumptionRequester(
                userId,
                RolUsuario.ALUMNO,
                studentId
        );
        givenPurchaseExists();
        givenAuthenticatedRequester(requester);

        assertThrows(PurchaseAccessDeniedException.class,
                this::whenGettingPurchaseStatus);
    }

    @Test
    void execute_WhenTutorCannotAccessStudent_ShouldThrowException() {
        ConsumptionRequester requester = new ConsumptionRequester(
                userId,
                RolUsuario.PADRE,
                null
        );
        givenPurchaseExists();
        givenAuthenticatedRequester(requester);
        givenTutorCanAccessStudent(false);

        assertThrows(PurchaseAccessDeniedException.class,
                this::whenGettingPurchaseStatus);

        verify(accessRepository).tutorCanAccessStudent(userId, studentId);
    }

    @Test
    void execute_WhenRoleCannotAccessPurchases_ShouldThrowException() {
        ConsumptionRequester requester = new ConsumptionRequester(
                userId,
                RolUsuario.ADMIN,
                null
        );
        givenPurchaseExists();
        givenAuthenticatedRequester(requester);

        assertThrows(PurchaseAccessDeniedException.class,
                this::whenGettingPurchaseStatus);
    }

    @Test
    void execute_WhenPurchaseDoesNotExist_ShouldThrowException() {
        givenPurchaseDoesNotExist();

        assertThrows(PurchaseNotFoundException.class,
                this::whenGettingPurchaseStatus);

        verifyNoInteractions(accessRepository);
    }

    private void givenPurchaseExists() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
    }

    private void givenPurchaseDoesNotExist() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.empty());
    }

    private void givenAuthenticatedRequester(ConsumptionRequester requester) {
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(requester));
    }

    private void givenAuthenticatedRequesterDoesNotExist() {
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.empty());
    }

    private void givenTutorCanAccessStudent(boolean canAccess) {
        when(accessRepository.tutorCanAccessStudent(userId, studentId)).thenReturn(canAccess);
    }

    private GetPurchaseStatusResponse whenGettingPurchaseStatus() {
        return useCase.execute(request());
    }

    private GetPurchaseStatusRequest request() {
        return new GetPurchaseStatusRequest(COGNITO_SUBJECT, purchaseId);
    }
}
