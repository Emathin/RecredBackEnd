package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferBalanceBetweenChildrenTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private com.recred.backend.domain.repositories.TutorRepository tutorRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private com.recred.backend.domain.repositories.MovimientoBilleteraRepository movimientoBilleteraRepository;

    @InjectMocks
    private TransferBalanceBetweenChildren transferUseCase;

    private String cognitoSub;
    private UUID fromId;
    private UUID toId;
    private Usuario parentUser;
    private Tutor tutor;
    private Billetera fromWallet;
    private Billetera toWallet;

    @BeforeEach
    void setUp() {
        cognitoSub = "sub-123";
        fromId = UUID.randomUUID();
        toId = UUID.randomUUID();

        Alumno child1 = new Alumno();
        child1.setId(fromId);
        child1.setNombre("Hijo");
        child1.setApellido("Uno");

        Alumno child2 = new Alumno();
        child2.setId(toId);
        child2.setNombre("Hijo");
        child2.setApellido("Dos");

        tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        tutor.setHijos(Set.of(child1, child2));

        parentUser = new Usuario();
        parentUser.setId(UUID.randomUUID());
        parentUser.setRol(RolUsuario.PADRE);
        parentUser.setTutor(tutor);

        fromWallet = new Billetera();
        fromWallet.setId(UUID.randomUUID());
        fromWallet.setSaldoCreditos(new BigDecimal("100.00"));

        toWallet = new Billetera();
        toWallet.setId(UUID.randomUUID());
        toWallet.setSaldoCreditos(new BigDecimal("50.00"));
    }

    @Test
    void execute_shouldTransferBalanceSuccessfully_whenAllValidationsPass() {
        BigDecimal amount = new BigDecimal("30.00");

        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.of(parentUser));
        when(tutorRepository.findByUsuarioId(parentUser.getId())).thenReturn(Optional.of(tutor));
        when(walletRepository.findByStudentId(fromId)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByStudentId(toId)).thenReturn(Optional.of(toWallet));

        transferUseCase.execute(cognitoSub, fromId, toId, amount);

        assertEquals(new BigDecimal("70.00"), fromWallet.getSaldoCreditos());
        assertEquals(new BigDecimal("80.00"), toWallet.getSaldoCreditos());
        verify(walletRepository).save(fromWallet);
        verify(walletRepository).save(toWallet);
        verify(movimientoBilleteraRepository).registrarMovimiento(
                org.mockito.ArgumentMatchers.eq(fromWallet.getId()),
                org.mockito.ArgumentMatchers.eq(com.recred.backend.domain.models.TipoMovimientoBilletera.AJUSTE),
                org.mockito.ArgumentMatchers.eq(amount.negate()),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(parentUser.getId())
        );
        verify(movimientoBilleteraRepository).registrarMovimiento(
                org.mockito.ArgumentMatchers.eq(toWallet.getId()),
                org.mockito.ArgumentMatchers.eq(com.recred.backend.domain.models.TipoMovimientoBilletera.AJUSTE),
                org.mockito.ArgumentMatchers.eq(amount),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(parentUser.getId())
        );
    }

    @Test
    void execute_shouldThrowException_whenAmountIsZeroOrLess() {
        assertThrows(IllegalArgumentException.class, () ->
                transferUseCase.execute(cognitoSub, fromId, toId, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () ->
                transferUseCase.execute(cognitoSub, fromId, toId, new BigDecimal("-5")));

        verify(walletRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenFromStudentAndToStudentAreSame() {
        assertThrows(IllegalArgumentException.class, () ->
                transferUseCase.execute(cognitoSub, fromId, fromId, new BigDecimal("30.00")));

        verify(walletRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenUserNotFound() {
        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () ->
                transferUseCase.execute(cognitoSub, fromId, toId, new BigDecimal("30.00")));

        verify(walletRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenUserNotAParent() {
        parentUser.setRol(RolUsuario.ALUMNO);
        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.of(parentUser));

        assertThrows(SecurityException.class, () ->
                transferUseCase.execute(cognitoSub, fromId, toId, new BigDecimal("30.00")));

        verify(walletRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenUserHasNoTutor() {
        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.of(parentUser));
        when(tutorRepository.findByUsuarioId(parentUser.getId())).thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () ->
                transferUseCase.execute(cognitoSub, fromId, toId, new BigDecimal("30.00")));

        verify(walletRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenOneChildNotOwnedByTutor() {
        Alumno child1 = new Alumno();
        child1.setId(fromId);
        child1.setNombre("Hijo");
        child1.setApellido("Uno");
        tutor.setHijos(Set.of(child1));

        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.of(parentUser));
        when(tutorRepository.findByUsuarioId(parentUser.getId())).thenReturn(Optional.of(tutor));

        assertThrows(SecurityException.class, () ->
                transferUseCase.execute(cognitoSub, fromId, toId, new BigDecimal("30.00")));

        verify(walletRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenSourceWalletNotFound() {
        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.of(parentUser));
        when(tutorRepository.findByUsuarioId(parentUser.getId())).thenReturn(Optional.of(tutor));
        when(walletRepository.findByStudentId(fromId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                transferUseCase.execute(cognitoSub, fromId, toId, new BigDecimal("30.00")));

        verify(walletRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenDestinationWalletNotFound() {
        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.of(parentUser));
        when(tutorRepository.findByUsuarioId(parentUser.getId())).thenReturn(Optional.of(tutor));
        when(walletRepository.findByStudentId(fromId)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByStudentId(toId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                transferUseCase.execute(cognitoSub, fromId, toId, new BigDecimal("30.00")));

        verify(walletRepository, never()).save(any());
    }
}
