package com.recred.backend.unit.application;

import com.recred.backend.application.TransferBalanceBetweenChildren;
import com.recred.backend.application.TransferBalanceBetweenChildrenRequest;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.SchoolCredit;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.SchoolCreditRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferBalanceBetweenChildrenTest {

    private static final String COGNITO_SUB = "sub-123";
    private static final BigDecimal TRANSFER_AMOUNT = new BigDecimal("30.00");

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final TutorRepository tutorRepository = mock(TutorRepository.class);
    private final WalletRepository walletRepository = mock(WalletRepository.class);
    private final MovimientoBilleteraRepository movimientoBilleteraRepository =
            mock(MovimientoBilleteraRepository.class);
    private final SchoolCreditRepository schoolCreditRepository =
            mock(SchoolCreditRepository.class);
    private final TransferBalanceBetweenChildren useCase =
            new TransferBalanceBetweenChildren(
                    usuarioRepository,
                    tutorRepository,
                    walletRepository,
                    movimientoBilleteraRepository,
                    schoolCreditRepository
            );

    private TransferBalanceBetweenChildrenRequest request;
    private Throwable exception;
    private UUID fromStudentId;
    private UUID toStudentId;
    private Usuario parentUser;
    private Tutor tutor;
    private Alumno fromStudent;
    private Alumno toStudent;
    private Billetera fromWallet;
    private Billetera toWallet;
    private SchoolCredit activeCredit;

    @Test
    void execute_WhenAllValidationsPass_TransfersBalance() {
        givenValidTransferScenario();
        givenNoActiveMicrocredit();

        whenTransferringBalance();

        thenWalletBalancesAre("70.00", "80.00");
        thenWalletsWereSaved();
        thenTransferMovementsWereRegistered();
    }

    @Test
    void execute_WhenActiveMicrocreditIsLowerThanTransfer_PaysCreditOff() {
        givenValidTransferScenario();
        givenActiveMicrocredit("20.00");

        whenTransferringBalance();

        thenWalletBalancesAre("70.00", "60.00");
        thenMicrocreditWasPaidOff();
        thenMicrocreditPaymentWasRegistered("20.00");
    }

    @Test
    void execute_WhenActiveMicrocreditIsHigherThanTransfer_ReducesCredit() {
        givenValidTransferScenario();
        givenActiveMicrocredit("50.00");

        whenTransferringBalance();

        thenWalletBalancesAre("70.00", "50.00");
        thenMicrocreditWasReducedTo("20.00");
    }

    @Test
    void execute_WhenUserDoesNotExist_ThrowsException() {
        givenValidRequest();
        givenAuthenticatedUserDoesNotExist();

        whenTransferringBalanceFails(SecurityException.class);

        thenWalletsWereNotSaved();
    }

    @Test
    void execute_WhenUserIsNotParent_ThrowsException() {
        givenValidRequest();
        givenAuthenticatedUserIsNotParent();

        whenTransferringBalanceFails(SecurityException.class);

        thenWalletsWereNotSaved();
    }

    @Test
    void execute_WhenUserHasNoTutor_ThrowsException() {
        givenValidRequest();
        givenAuthenticatedParent();
        givenTutorDoesNotExist();

        whenTransferringBalanceFails(SecurityException.class);

        thenWalletsWereNotSaved();
    }

    @Test
    void execute_WhenChildDoesNotBelongToTutor_ThrowsException() {
        givenValidRequest();
        givenAuthenticatedParent();
        givenTutorWithOnlySourceChild();

        whenTransferringBalanceFails(SecurityException.class);

        thenWalletsWereNotSaved();
    }

    @Test
    void execute_WhenSourceWalletDoesNotExist_ThrowsException() {
        givenValidRequest();
        givenAuthenticatedParentWithChildren();
        givenSourceWalletDoesNotExist();

        whenTransferringBalanceFails(RuntimeException.class);

        thenWalletsWereNotSaved();
    }

    @Test
    void execute_WhenDestinationWalletDoesNotExist_ThrowsException() {
        givenValidRequest();
        givenAuthenticatedParentWithChildren();
        givenSourceWalletExists();
        givenDestinationWalletDoesNotExist();

        whenTransferringBalanceFails(RuntimeException.class);

        thenWalletsWereNotSaved();
    }

    @Test
    void execute_WhenUserHasFreePlan_ThrowsException() {
        givenValidRequest();
        givenAuthenticatedFreePlanParent();

        whenTransferringBalanceFails(IllegalArgumentException.class);

        thenErrorMessageIs(
                "La transferencia entre hijos no está permitida en cuentas gratuitas"
        );
        thenWalletsWereNotSaved();
    }

    @Test
    void execute_WhenCollaboratorExceedsNetBalance_ThrowsException() {
        givenValidRequest();
        givenAuthenticatedParentWithChildren();
        tutor.setEsColaborador(true);
        givenWalletsExist();
        
        when(movimientoBilleteraRepository.getCollaboratorNetBalance(fromWallet.getId(), parentUser.getId()))
                .thenReturn(new BigDecimal("20.00"));

        whenTransferringBalanceFails(SecurityException.class);

        thenErrorMessageIs("No puedes transferir más de lo que has aportado a esta billetera (20.00)");
        thenWalletsWereNotSaved();
    }

    @Test
    void execute_WhenCollaboratorHasSufficientNetBalance_TransfersBalance() {
        givenValidTransferScenario();
        tutor.setEsColaborador(true);
        givenNoActiveMicrocredit();
        
        when(movimientoBilleteraRepository.getCollaboratorNetBalance(fromWallet.getId(), parentUser.getId()))
                .thenReturn(new BigDecimal("50.00"));

        whenTransferringBalance();

        thenWalletBalancesAre("70.00", "80.00");
        thenWalletsWereSaved();
        thenTransferMovementsWereRegistered();
    }

    private void givenValidTransferScenario() {
        givenValidRequest();
        givenAuthenticatedParentWithChildren();
        givenWalletsExist();
    }

    private void givenValidRequest() {
        fromStudentId = randomUUID();
        toStudentId = randomUUID();
        request = request(fromStudentId, toStudentId, TRANSFER_AMOUNT);
    }

    private void givenAuthenticatedParentWithChildren() {
        givenAuthenticatedParent();
        givenTutorWithChildren();
    }

    private void givenAuthenticatedParent() {
        parentUser = user(RolUsuario.PADRE, TipoPlan.AVANZADO);
        when(usuarioRepository.findByIdCognito(COGNITO_SUB))
                .thenReturn(Optional.of(parentUser));
    }

    private void givenAuthenticatedFreePlanParent() {
        parentUser = user(RolUsuario.PADRE, TipoPlan.GRATUITO);
        when(usuarioRepository.findByIdCognito(COGNITO_SUB))
                .thenReturn(Optional.of(parentUser));
    }

    private void givenAuthenticatedUserIsNotParent() {
        parentUser = user(RolUsuario.ALUMNO, TipoPlan.AVANZADO);
        when(usuarioRepository.findByIdCognito(COGNITO_SUB))
                .thenReturn(Optional.of(parentUser));
    }

    private void givenAuthenticatedUserDoesNotExist() {
        when(usuarioRepository.findByIdCognito(COGNITO_SUB)).thenReturn(Optional.empty());
    }

    private void givenTutorWithChildren() {
        fromStudent = student(fromStudentId, "Hijo", "Uno");
        toStudent = student(toStudentId, "Hijo", "Dos");
        tutor = tutor(Set.of(fromStudent, toStudent));
        when(tutorRepository.findByUsuarioId(parentUser.getId())).thenReturn(Optional.of(tutor));
    }

    private void givenTutorWithOnlySourceChild() {
        fromStudent = student(fromStudentId, "Hijo", "Uno");
        tutor = tutor(Set.of(fromStudent));
        when(tutorRepository.findByUsuarioId(parentUser.getId())).thenReturn(Optional.of(tutor));
    }

    private void givenTutorDoesNotExist() {
        when(tutorRepository.findByUsuarioId(parentUser.getId())).thenReturn(Optional.empty());
    }

    private void givenWalletsExist() {
        givenSourceWalletExists();
        givenDestinationWalletExists();
    }

    private void givenSourceWalletExists() {
        fromWallet = wallet("100.00");
        when(walletRepository.findByStudentId(fromStudentId)).thenReturn(Optional.of(fromWallet));
    }

    private void givenDestinationWalletExists() {
        toWallet = wallet("50.00");
        when(walletRepository.findByStudentId(toStudentId)).thenReturn(Optional.of(toWallet));
    }

    private void givenSourceWalletDoesNotExist() {
        when(walletRepository.findByStudentId(fromStudentId)).thenReturn(Optional.empty());
    }

    private void givenDestinationWalletDoesNotExist() {
        when(walletRepository.findByStudentId(toStudentId)).thenReturn(Optional.empty());
    }

    private void givenNoActiveMicrocredit() {
        when(schoolCreditRepository.findByStudentIdAndStatus(toStudentId, CreditStatus.ACTIVE))
                .thenReturn(Optional.empty());
    }

    private void givenActiveMicrocredit(String amount) {
        activeCredit = new SchoolCredit();
        activeCredit.setId(randomUUID());
        activeCredit.setAmount(new BigDecimal(amount));
        activeCredit.setStatus(CreditStatus.ACTIVE);
        when(schoolCreditRepository.findByStudentIdAndStatus(toStudentId, CreditStatus.ACTIVE))
                .thenReturn(Optional.of(activeCredit));
    }

    private void whenTransferringBalance() {
        useCase.execute(request);
    }

    private <T extends Throwable> void whenTransferringBalanceFails(
            Class<T> expectedException
    ) {
        Executable execution = () -> useCase.execute(request);
        exception = assertThrows(expectedException, execution);
    }

    private void thenWalletBalancesAre(
            String expectedSourceBalance,
            String expectedDestinationBalance
    ) {
        assertEquals(new BigDecimal(expectedSourceBalance), fromWallet.getSaldoCreditos());
        assertEquals(new BigDecimal(expectedDestinationBalance), toWallet.getSaldoCreditos());
    }

    private void thenWalletsWereSaved() {
        verify(walletRepository).save(fromWallet);
        verify(walletRepository).save(toWallet);
    }

    private void thenWalletsWereNotSaved() {
        verify(walletRepository, never()).save(any());
    }

    private void thenTransferMovementsWereRegistered() {
        verify(movimientoBilleteraRepository).registrarMovimiento(
                eq(fromWallet.getId()),
                eq(TipoMovimientoBilletera.AJUSTE),
                eq(TRANSFER_AMOUNT.negate()),
                anyString(),
                eq(parentUser.getId())
        );
        verify(movimientoBilleteraRepository).registrarMovimiento(
                eq(toWallet.getId()),
                eq(TipoMovimientoBilletera.AJUSTE),
                eq(TRANSFER_AMOUNT),
                anyString(),
                eq(parentUser.getId())
        );
    }

    private void thenMicrocreditWasPaidOff() {
        assertEquals(BigDecimal.ZERO, activeCredit.getAmount());
        assertEquals(CreditStatus.PAID, activeCredit.getStatus());
        verify(schoolCreditRepository).save(activeCredit);
    }

    private void thenMicrocreditWasReducedTo(String expectedAmount) {
        assertEquals(new BigDecimal(expectedAmount), activeCredit.getAmount());
        assertEquals(CreditStatus.ACTIVE, activeCredit.getStatus());
        verify(schoolCreditRepository).save(activeCredit);
    }

    private void thenMicrocreditPaymentWasRegistered(String expectedAmount) {
        verify(movimientoBilleteraRepository).registrarMovimiento(
                eq(toWallet.getId()),
                eq(TipoMovimientoBilletera.AJUSTE),
                eq(new BigDecimal(expectedAmount).negate()),
                anyString(),
                eq(parentUser.getId())
        );
    }

    private void thenErrorMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, exception.getMessage());
    }

    private TransferBalanceBetweenChildrenRequest request(
            UUID sourceStudentId,
            UUID destinationStudentId,
            BigDecimal amount
    ) {
        return new TransferBalanceBetweenChildrenRequest(
                COGNITO_SUB,
                sourceStudentId,
                destinationStudentId,
                amount
        );
    }

    private Usuario user(RolUsuario role, TipoPlan plan) {
        Usuario user = new Usuario();
        user.setId(randomUUID());
        user.setRol(role);
        user.setPlan(plan);
        return user;
    }

    private Tutor tutor(Set<Alumno> children) {
        Tutor tutor = new Tutor();
        tutor.setId(randomUUID());
        tutor.setHijos(children);
        return tutor;
    }

    private Alumno student(UUID id, String name, String lastName) {
        Alumno student = new Alumno();
        student.setId(id);
        student.setNombre(name);
        student.setApellido(lastName);
        return student;
    }

    private Billetera wallet(String balance) {
        Billetera wallet = new Billetera();
        wallet.setId(randomUUID());
        wallet.setSaldoCreditos(new BigDecimal(balance));
        return wallet;
    }
}
