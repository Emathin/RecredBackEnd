package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.domain.exceptions.PurchaseAccessDeniedException;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ConsumptionRequester;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.repositories.ConsumptionAccessRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.infrastructure.web.KiosqueroAccessGuard;
import com.recred.backend.infrastructure.web.PurchaseAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseAccessGuardTest {

    private static final String COGNITO_SUBJECT = "subject";
    private static final UUID PURCHASE_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID BUFFET_ID = UUID.randomUUID();

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private ConsumptionAccessRepository consumptionAccessRepository;

    @Mock
    private KiosqueroAccessGuard kiosqueroAccessGuard;

    @Mock
    private Authentication authentication;

    private PurchaseAccessGuard guard;
    private Purchase defaultPurchase;

    @BeforeEach
    void setUp() {
        guard = new PurchaseAccessGuard(
                purchaseRepository,
                consumptionAccessRepository,
                kiosqueroAccessGuard
        );
        defaultPurchase = buildDefaultPurchase();
    }

    @Test
    void requirePurchaseAccess_WhenPurchaseDoesNotExist_ShouldFail() {
        givenPurchaseDoesNotExist();

        Executable execution = this::whenRequiringPurchaseAccess;

        thenThrowsPurchaseNotFoundException(execution);
    }

    @Test
    void requirePurchaseAccess_WhenAuthenticationIsMissing_ShouldFail() {
        givenPurchaseExists();

        Executable execution = this::whenRequiringPurchaseAccessWithNullAuthentication;

        thenThrowsAuthenticationCredentialsNotFoundException(execution);
    }

    @Test
    void requirePurchaseAccess_WhenRequesterDoesNotExist_ShouldFail() {
        givenPurchaseExists();
        givenAuthenticationSubject();
        givenRequesterDoesNotExist();

        Executable execution = this::whenRequiringPurchaseAccess;

        thenThrowsUsuarioAutenticadoNoEncontradoException(execution);
    }

    @Test
    void requirePurchaseAccess_WhenRequesterIsSameStudent_ShouldReturnPurchase() {
        givenPurchaseExists();
        givenAuthenticationSubject();
        givenRequesterIsStudent(STUDENT_ID);

        Purchase result = guard.requirePurchaseAccess(authentication, PURCHASE_ID);

        thenPurchaseIsReturned(result);
        thenBuffetAccessIsNotValidated();
    }

    @Test
    void requirePurchaseAccess_WhenRequesterIsTutorWithAccess_ShouldReturnPurchase() {
        givenPurchaseExists();
        givenAuthenticationSubject();
        givenRequesterIsTutor();
        givenTutorCanAccessStudent(true);

        Purchase result = guard.requirePurchaseAccess(authentication, PURCHASE_ID);

        thenPurchaseIsReturned(result);
    }

    @Test
    void requirePurchaseAccess_WhenRequesterIsTutorWithoutAccess_ShouldFail() {
        givenPurchaseExists();
        givenAuthenticationSubject();
        givenRequesterIsTutor();
        givenTutorCanAccessStudent(false);

        Executable execution = this::whenRequiringPurchaseAccess;

        thenThrowsPurchaseAccessDeniedException(execution);
    }

    @Test
    void requirePurchaseAccess_WhenRequesterIsAdmin_ShouldRequireBuffetAccess() {
        givenPurchaseExists();
        givenAuthenticationSubject();
        givenRequesterIsAdmin();

        Purchase result = guard.requirePurchaseAccess(authentication, PURCHASE_ID);

        thenPurchaseIsReturnedViaEquals(result);
        thenBuffetAccessIsValidated();
    }

    @Test
    void requirePurchaseAccess_WhenPurchaseHasNoStudent_ShouldDenyStudentRequester() {
        givenPurchaseWithoutStudentExists();
        givenAuthenticationSubject();
        givenRequesterIsStudent(STUDENT_ID);

        Executable execution = this::whenRequiringPurchaseAccess;

        thenThrowsPurchaseAccessDeniedException(execution);
    }

    private void givenPurchaseDoesNotExist() {
        when(purchaseRepository.findById(PURCHASE_ID)).thenReturn(Optional.empty());
    }

    private void givenPurchaseExists() {
        when(purchaseRepository.findById(PURCHASE_ID)).thenReturn(Optional.of(defaultPurchase));
    }

    private void givenPurchaseWithoutStudentExists() {
        defaultPurchase.setStudent(null);
        when(purchaseRepository.findById(PURCHASE_ID)).thenReturn(Optional.of(defaultPurchase));
    }

    private void givenAuthenticationSubject() {
        when(authentication.getName()).thenReturn(COGNITO_SUBJECT);
    }

    private void givenRequesterDoesNotExist() {
        when(consumptionAccessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.empty());
    }

    private void givenRequesterIsStudent(UUID associatedStudentId) {
        when(consumptionAccessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        USER_ID,
                        RolUsuario.ALUMNO,
                        associatedStudentId
                )));
    }

    private void givenRequesterIsTutor() {
        when(consumptionAccessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        USER_ID,
                        RolUsuario.PADRE,
                        null
                )));
    }

    private void givenTutorCanAccessStudent(boolean canAccess) {
        when(consumptionAccessRepository.tutorCanAccessStudent(USER_ID, STUDENT_ID))
                .thenReturn(canAccess);
    }

    private void givenRequesterIsAdmin() {
        when(consumptionAccessRepository.findRequesterByCognitoSubject(COGNITO_SUBJECT))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        USER_ID,
                        RolUsuario.ADMIN,
                        null
                )));
    }

    private void whenRequiringPurchaseAccess() {
        guard.requirePurchaseAccess(authentication, PURCHASE_ID);
    }

    private void whenRequiringPurchaseAccessWithNullAuthentication() {
        guard.requirePurchaseAccess(null, PURCHASE_ID);
    }

    private void thenThrowsPurchaseNotFoundException(Executable execution) {
        assertThrows(PurchaseNotFoundException.class, execution);
    }

    private void thenThrowsAuthenticationCredentialsNotFoundException(Executable execution) {
        assertThrows(AuthenticationCredentialsNotFoundException.class, execution);
    }

    private void thenThrowsUsuarioAutenticadoNoEncontradoException(Executable execution) {
        assertThrows(UsuarioAutenticadoNoEncontradoException.class, execution);
    }

    private void thenThrowsPurchaseAccessDeniedException(Executable execution) {
        assertThrows(PurchaseAccessDeniedException.class, execution);
    }

    private void thenPurchaseIsReturned(Purchase result) {
        assertSame(defaultPurchase, result);
    }

    private void thenPurchaseIsReturnedViaEquals(Purchase result) {
        assertEquals(defaultPurchase, result);
    }

    private void thenBuffetAccessIsNotValidated() {
        verify(kiosqueroAccessGuard, never()).requireBuffetAccess(authentication, BUFFET_ID);
    }

    private void thenBuffetAccessIsValidated() {
        verify(kiosqueroAccessGuard).requireBuffetAccess(authentication, BUFFET_ID);
    }

    private Purchase buildDefaultPurchase() {
        Alumno student = new Alumno();
        student.setId(STUDENT_ID);
        Purchase newPurchase = new Purchase();
        newPurchase.setId(PURCHASE_ID);
        newPurchase.setStudent(student);
        newPurchase.setBuffetId(BUFFET_ID);
        return newPurchase;
    }
}
