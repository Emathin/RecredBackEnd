package com.recred.backend.unit.application;

import com.recred.backend.application.LoadBalanceToChild;
import com.recred.backend.application.LoadBalanceToChildRequest;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoadBalanceToChildTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private MovimientoBilleteraRepository movementRepository;

    private LoadBalanceToChild useCase;

    private String cognitoSub;
    private UUID studentId;
    private BigDecimal amount;
    private Usuario parentUser;
    private Tutor tutor;
    private Alumno student;
    private Billetera wallet;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new LoadBalanceToChild(
                usuarioRepository,
                tutorRepository,
                walletRepository,
                movementRepository
        );
    }

    @Test
    void givenParentCanLoadBalanceToChild_whenLoadingBalance_thenCreditsWallet() {
        givenLoadBalanceScenario("50.00");
        givenParentCanLoadBalanceToChild();
        givenStudentWalletExists();

        whenLoadingBalance();

        thenWalletBalanceIs("150.00");
        thenWalletWasSaved();
        thenLoadMovementWasRegistered();
    }

    @Test
    void givenZeroAmount_whenLoadingBalance_thenFailsBeforeSavingWallet() {
        givenLoadBalanceScenario("0.00");

        whenLoadingBalanceFails();

        thenLoadBalanceRequestIsInvalid();
        thenWalletWasNotSaved();
    }

    @Test
    void givenNegativeAmount_whenLoadingBalance_thenFailsBeforeSavingWallet() {
        givenLoadBalanceScenario("-10.00");

        whenLoadingBalanceFails();

        thenLoadBalanceRequestIsInvalid();
        thenWalletWasNotSaved();
    }

    @Test
    void givenParentUserDoesNotExist_whenLoadingBalance_thenFailsBeforeSavingWallet() {
        givenLoadBalanceScenario("50.00");
        givenParentUserDoesNotExist();

        whenLoadingBalanceFails();

        thenSecurityExceptionWasThrown();
        thenWalletWasNotSaved();
    }

    @Test
    void givenAuthenticatedUserIsNotParent_whenLoadingBalance_thenFailsBeforeSavingWallet() {
        givenLoadBalanceScenario("50.00");
        givenAuthenticatedUserIsNotParent();

        whenLoadingBalanceFails();

        thenSecurityExceptionWasThrown();
        thenWalletWasNotSaved();
    }

    @Test
    void givenStudentIsNotTutorChild_whenLoadingBalance_thenFailsBeforeSavingWallet() {
        givenLoadBalanceScenario("50.00");
        givenTutorHasNoChildren();

        whenLoadingBalanceFails();

        thenSecurityExceptionWasThrown();
        thenWalletWasNotSaved();
    }

    @Test
    void givenChildWalletDoesNotExist_whenLoadingBalance_thenFailsBeforeSavingWallet() {
        givenLoadBalanceScenario("50.00");
        givenParentCanLoadBalanceToChild();
        givenStudentWalletDoesNotExist();

        whenLoadingBalanceFails();

        thenWalletNotFoundWasThrown();
        thenWalletWasNotSaved();
    }

    private void givenLoadBalanceScenario(String amountValue) {
        cognitoSub = "sub-123";
        studentId = UUID.randomUUID();
        amount = new BigDecimal(amountValue);
        student = student(studentId);
        tutor = tutorWith(student);
        parentUser = parentUserWith(tutor);
        wallet = walletWithBalance("100.00");
    }

    private void givenParentCanLoadBalanceToChild() {
        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.of(parentUser));
        when(tutorRepository.findByUsuarioId(parentUser.getId())).thenReturn(Optional.of(tutor));
    }

    private void givenParentUserDoesNotExist() {
        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.empty());
    }

    private void givenAuthenticatedUserIsNotParent() {
        parentUser.setRol(RolUsuario.ALUMNO);
        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.of(parentUser));
    }

    private void givenTutorHasNoChildren() {
        tutor.setHijos(Set.of());
        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.of(parentUser));
        when(tutorRepository.findByUsuarioId(parentUser.getId())).thenReturn(Optional.of(tutor));
    }

    private void givenStudentWalletExists() {
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
    }

    private void givenStudentWalletDoesNotExist() {
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.empty());
    }

    private void whenLoadingBalance() {
        useCase.execute(request());
    }

    private void whenLoadingBalanceFails() {
        thrownException = assertThrows(RuntimeException.class, this::whenLoadingBalance);
    }

    private void thenWalletBalanceIs(String expectedBalance) {
        assertEquals(new BigDecimal(expectedBalance), wallet.getSaldoCreditos());
    }

    private void thenWalletWasSaved() {
        verify(walletRepository).save(wallet);
    }

    private void thenLoadMovementWasRegistered() {
        verify(movementRepository).registrarMovimiento(
                eq(wallet.getId()),
                eq(TipoMovimientoBilletera.CARGA),
                eq(amount),
                anyString(),
                eq(parentUser.getId())
        );
    }

    private void thenLoadBalanceRequestIsInvalid() {
        assertTrue(thrownException instanceof IllegalArgumentException);
    }

    private void thenSecurityExceptionWasThrown() {
        assertTrue(thrownException instanceof SecurityException);
    }

    private void thenWalletNotFoundWasThrown() {
        assertTrue(thrownException instanceof RuntimeException);
    }

    private void thenWalletWasNotSaved() {
        verify(walletRepository, never()).save(any());
    }

    private LoadBalanceToChildRequest request() {
        return new LoadBalanceToChildRequest(cognitoSub, studentId, amount);
    }

    private Alumno student(UUID studentId) {
        Alumno student = new Alumno();
        student.setId(studentId);
        student.setNombre("NombreHijo");
        student.setApellido("ApellidoHijo");
        return student;
    }

    private Tutor tutorWith(Alumno student) {
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        tutor.setHijos(Set.of(student));
        return tutor;
    }

    private Usuario parentUserWith(Tutor tutor) {
        Usuario parentUser = new Usuario();
        parentUser.setId(UUID.randomUUID());
        parentUser.setRol(RolUsuario.PADRE);
        parentUser.setTutor(tutor);
        return parentUser;
    }

    private Billetera walletWithBalance(String balance) {
        Billetera wallet = new Billetera();
        wallet.setId(UUID.randomUUID());
        wallet.setSaldoCreditos(new BigDecimal(balance));
        return wallet;
    }
}
