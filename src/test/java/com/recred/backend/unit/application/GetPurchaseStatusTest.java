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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    private GetPurchaseStatus useCase;

    private UUID purchaseId;
    private UUID studentId;
    private UUID userId;
    private Purchase purchase;
    private ConsumptionRequester requester;
    private GetPurchaseStatusResponse response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new GetPurchaseStatus(purchaseRepository, accessRepository);
    }

    @Test
    void givenStudentOwnsPurchase_whenGettingPurchaseStatus_thenReturnsStatus() {
        givenPurchaseStatusScenario();
        givenRequesterIsStudentOwner();
        givenPurchaseExists();
        givenAuthenticatedRequesterExists();

        whenGettingPurchaseStatus();

        thenPurchaseStatusIsReturned();
        thenTutorAccessWasNotChecked();
    }

    @Test
    void givenTutorCanAccessStudent_whenGettingPurchaseStatus_thenReturnsStatus() {
        givenPurchaseStatusScenario();
        givenRequesterIsTutor();
        givenPurchaseExists();
        givenAuthenticatedRequesterExists();
        givenTutorCanAccessStudent();

        whenGettingPurchaseStatus();

        thenPurchaseStatusIsReturned();
        thenTutorAccessWasChecked();
    }

    @Test
    void givenAuthenticatedUserDoesNotExist_whenGettingPurchaseStatus_thenFails() {
        givenPurchaseStatusScenario();
        givenPurchaseExists();
        givenAuthenticatedRequesterDoesNotExist();

        whenGettingPurchaseStatusFails();

        thenAuthenticatedUserNotFoundIsThrown();
    }

    @Test
    void givenRequesterCannotAccessStudent_whenGettingPurchaseStatus_thenFails() {
        givenPurchaseStatusScenario();
        givenRequesterIsStudentForAnotherStudent();
        givenPurchaseExists();
        givenAuthenticatedRequesterExists();

        whenGettingPurchaseStatusFails();

        thenPurchaseAccessIsDenied();
    }

    @Test
    void givenPurchaseWithoutStudent_whenGettingPurchaseStatus_thenFails() {
        givenPurchaseStatusScenario();
        givenPurchaseHasNoStudent();
        givenRequesterIsStudentOwner();
        givenPurchaseExists();
        givenAuthenticatedRequesterExists();

        whenGettingPurchaseStatusFails();

        thenPurchaseAccessIsDenied();
    }

    @Test
    void givenPurchaseStudentWithoutId_whenGettingPurchaseStatus_thenFails() {
        givenPurchaseStatusScenario();
        givenPurchaseStudentHasNoId();
        givenRequesterIsStudentOwner();
        givenPurchaseExists();
        givenAuthenticatedRequesterExists();

        whenGettingPurchaseStatusFails();

        thenPurchaseAccessIsDenied();
    }

    @Test
    void givenTutorCannotAccessStudent_whenGettingPurchaseStatus_thenFails() {
        givenPurchaseStatusScenario();
        givenRequesterIsTutor();
        givenPurchaseExists();
        givenAuthenticatedRequesterExists();
        givenTutorCannotAccessStudent();

        whenGettingPurchaseStatusFails();

        thenPurchaseAccessIsDenied();
        thenTutorAccessWasChecked();
    }

    @Test
    void givenUnsupportedRole_whenGettingPurchaseStatus_thenFails() {
        givenPurchaseStatusScenario();
        givenRequesterHasUnsupportedRole();
        givenPurchaseExists();
        givenAuthenticatedRequesterExists();

        whenGettingPurchaseStatusFails();

        thenPurchaseAccessIsDenied();
    }

    @Test
    void givenMissingPurchase_whenGettingPurchaseStatus_thenFailsBeforeLoadingRequester() {
        givenPurchaseStatusScenario();
        givenPurchaseDoesNotExist();

        whenGettingPurchaseStatusFails();

        thenPurchaseNotFoundIsThrown();
        thenAuthenticatedRequesterWasNotRequested();
    }

    private void givenPurchaseStatusScenario() {
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

    private void givenRequesterIsStudentOwner() {
        requester = new ConsumptionRequester(userId, RolUsuario.ALUMNO, studentId);
    }

    private void givenRequesterIsTutor() {
        requester = new ConsumptionRequester(userId, RolUsuario.PADRE, null);
    }

    private void givenRequesterIsStudentForAnotherStudent() {
        requester = new ConsumptionRequester(userId, RolUsuario.ALUMNO, UUID.randomUUID());
    }

    private void givenRequesterHasUnsupportedRole() {
        requester = new ConsumptionRequester(userId, RolUsuario.ADMIN, null);
    }

    private void givenPurchaseHasNoStudent() {
        purchase.setStudent(null);
    }

    private void givenPurchaseStudentHasNoId() {
        purchase.getStudent().setId(null);
    }

    private void givenPurchaseExists() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
    }

    private void givenPurchaseDoesNotExist() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.empty());
    }

    private void givenAuthenticatedRequesterExists() {
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(requester));
    }

    private void givenAuthenticatedRequesterDoesNotExist() {
        when(accessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.empty());
    }

    private void givenTutorCanAccessStudent() {
        when(accessRepository.tutorCanAccessStudent(userId, studentId)).thenReturn(true);
    }

    private void givenTutorCannotAccessStudent() {
        when(accessRepository.tutorCanAccessStudent(userId, studentId)).thenReturn(false);
    }

    private void whenGettingPurchaseStatus() {
        response = useCase.execute(request());
    }

    private void whenGettingPurchaseStatusFails() {
        thrownException = assertThrows(RuntimeException.class, () -> useCase.execute(request()));
    }

    private void thenPurchaseStatusIsReturned() {
        assertEquals(purchaseId, response.purchaseId());
        assertEquals(studentId, response.studentId());
        assertEquals("PENDIENTE", response.status());
        assertEquals("A preparar", response.statusLabel());
    }

    private void thenTutorAccessWasNotChecked() {
        verify(accessRepository, never()).tutorCanAccessStudent(userId, studentId);
    }

    private void thenTutorAccessWasChecked() {
        verify(accessRepository).tutorCanAccessStudent(userId, studentId);
    }

    private void thenAuthenticatedUserNotFoundIsThrown() {
        assertTrue(thrownException instanceof UsuarioAutenticadoNoEncontradoException);
    }

    private void thenPurchaseAccessIsDenied() {
        assertTrue(thrownException instanceof PurchaseAccessDeniedException);
    }

    private void thenPurchaseNotFoundIsThrown() {
        assertTrue(thrownException instanceof PurchaseNotFoundException);
    }

    private void thenAuthenticatedRequesterWasNotRequested() {
        verifyNoInteractions(accessRepository);
    }

    private GetPurchaseStatusRequest request() {
        return new GetPurchaseStatusRequest(COGNITO_SUBJECT, purchaseId);
    }
}
