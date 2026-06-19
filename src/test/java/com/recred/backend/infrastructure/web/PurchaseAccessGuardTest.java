package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.exceptions.PurchaseAccessDeniedException;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ConsumptionRequester;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.repositories.ConsumptionAccessRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private ConsumptionAccessRepository consumptionAccessRepository;

    @Mock
    private KiosqueroAccessGuard kiosqueroAccessGuard;

    @Mock
    private Authentication authentication;

    private PurchaseAccessGuard guard;
    private UUID purchaseId;
    private UUID studentId;
    private UUID userId;
    private UUID buffetId;
    private Purchase purchase;

    @BeforeEach
    void setUp() {
        guard = new PurchaseAccessGuard(
                purchaseRepository,
                consumptionAccessRepository,
                kiosqueroAccessGuard
        );
        purchaseId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        userId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
        purchase = purchase();
    }

    @Test
    void requirePurchaseAccess_WhenPurchaseDoesNotExist_ShouldFail() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.empty());

        assertThrows(PurchaseNotFoundException.class,
                () -> guard.requirePurchaseAccess(authentication, purchaseId));
    }

    @Test
    void requirePurchaseAccess_WhenAuthenticationIsMissing_ShouldFail() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> guard.requirePurchaseAccess(null, purchaseId));
    }

    @Test
    void requirePurchaseAccess_WhenRequesterDoesNotExist_ShouldFail() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(authentication.getName()).thenReturn("subject");
        when(consumptionAccessRepository.findRequesterByCognitoSubject("subject"))
                .thenReturn(Optional.empty());

        assertThrows(UsuarioAutenticadoNoEncontradoException.class,
                () -> guard.requirePurchaseAccess(authentication, purchaseId));
    }

    @Test
    void requirePurchaseAccess_WhenRequesterIsSameStudent_ShouldReturnPurchase() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(authentication.getName()).thenReturn("subject");
        when(consumptionAccessRepository.findRequesterByCognitoSubject("subject"))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.ALUMNO,
                        studentId
                )));

        Purchase result = guard.requirePurchaseAccess(authentication, purchaseId);

        assertSame(purchase, result);
        verify(kiosqueroAccessGuard, never()).requireBuffetAccess(authentication, buffetId);
    }

    @Test
    void requirePurchaseAccess_WhenRequesterIsTutorWithAccess_ShouldReturnPurchase() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(authentication.getName()).thenReturn("subject");
        when(consumptionAccessRepository.findRequesterByCognitoSubject("subject"))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.PADRE,
                        null
                )));
        when(consumptionAccessRepository.tutorCanAccessStudent(userId, studentId))
                .thenReturn(true);

        Purchase result = guard.requirePurchaseAccess(authentication, purchaseId);

        assertSame(purchase, result);
    }

    @Test
    void requirePurchaseAccess_WhenRequesterIsTutorWithoutAccess_ShouldFail() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(authentication.getName()).thenReturn("subject");
        when(consumptionAccessRepository.findRequesterByCognitoSubject("subject"))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.PADRE,
                        null
                )));
        when(consumptionAccessRepository.tutorCanAccessStudent(userId, studentId))
                .thenReturn(false);

        assertThrows(PurchaseAccessDeniedException.class,
                () -> guard.requirePurchaseAccess(authentication, purchaseId));
    }

    @Test
    void requirePurchaseAccess_WhenRequesterIsAdmin_ShouldRequireBuffetAccess() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(authentication.getName()).thenReturn("subject");
        when(consumptionAccessRepository.findRequesterByCognitoSubject("subject"))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.ADMIN,
                        null
                )));

        Purchase result = guard.requirePurchaseAccess(authentication, purchaseId);

        assertEquals(purchase, result);
        verify(kiosqueroAccessGuard).requireBuffetAccess(authentication, buffetId);
    }

    @Test
    void requirePurchaseAccess_WhenPurchaseHasNoStudent_ShouldDenyStudentRequester() {
        purchase.setStudent(null);
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(authentication.getName()).thenReturn("subject");
        when(consumptionAccessRepository.findRequesterByCognitoSubject("subject"))
                .thenReturn(Optional.of(new ConsumptionRequester(
                        userId,
                        RolUsuario.ALUMNO,
                        studentId
                )));

        assertThrows(PurchaseAccessDeniedException.class,
                () -> guard.requirePurchaseAccess(authentication, purchaseId));
    }

    private Purchase purchase() {
        Alumno student = new Alumno();
        student.setId(studentId);
        Purchase newPurchase = new Purchase();
        newPurchase.setId(purchaseId);
        newPurchase.setStudent(student);
        newPurchase.setBuffetId(buffetId);
        return newPurchase;
    }
}
