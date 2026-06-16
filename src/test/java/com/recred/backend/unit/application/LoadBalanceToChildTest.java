package com.recred.backend.unit.application;

import com.recred.backend.application.LoadBalanceToChild;
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
class LoadBalanceToChildTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private com.recred.backend.domain.repositories.TutorRepository tutorRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private com.recred.backend.domain.repositories.MovimientoBilleteraRepository movimientoBilleteraRepository;

    @InjectMocks
    private LoadBalanceToChild loadBalanceToChild;

    private String cognitoSub;
    private UUID studentId;
    private Usuario parentUser;
    private Tutor tutor;
    private Alumno student;
    private Billetera wallet;

    @BeforeEach
    void setUp() {
        cognitoSub = "sub-123";
        studentId = UUID.randomUUID();

        student = new Alumno();
        student.setId(studentId);
        student.setNombre("NombreHijo");
        student.setApellido("ApellidoHijo");

        tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        tutor.setHijos(Set.of(student));

        parentUser = new Usuario();
        parentUser.setId(UUID.randomUUID());
        parentUser.setRol(RolUsuario.PADRE);
        parentUser.setTutor(tutor);

        wallet = new Billetera();
        wallet.setId(UUID.randomUUID());
        wallet.setSaldoCreditos(new BigDecimal("100.00"));
    }

    @Test
    void execute_shouldLoadBalanceSuccessfully_whenAllValidationsPass() {
        BigDecimal amount = new BigDecimal("50.00");

        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.of(parentUser));
        when(tutorRepository.findByUsuarioId(parentUser.getId())).thenReturn(Optional.of(tutor));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));

        loadBalanceToChild.execute(cognitoSub, studentId, amount);

        assertEquals(new BigDecimal("150.00"), wallet.getSaldoCreditos());
        verify(walletRepository).save(wallet);
        verify(movimientoBilleteraRepository).registrarMovimiento(
                org.mockito.ArgumentMatchers.eq(wallet.getId()),
                org.mockito.ArgumentMatchers.eq(com.recred.backend.domain.models.TipoMovimientoBilletera.CARGA),
                org.mockito.ArgumentMatchers.eq(amount),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(parentUser.getId())
        );
    }

    @Test
    void execute_shouldThrowException_whenAmountIsZeroOrLess() {
        assertThrows(IllegalArgumentException.class, () ->
                loadBalanceToChild.execute(cognitoSub, studentId, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () ->
                loadBalanceToChild.execute(cognitoSub, studentId, new BigDecimal("-10")));

        verify(walletRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenUserNotFound() {
        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () ->
                loadBalanceToChild.execute(cognitoSub, studentId, new BigDecimal("50.00")));

        verify(walletRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenUserIsNotAParent() {
        parentUser.setRol(RolUsuario.ALUMNO);
        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.of(parentUser));

        assertThrows(SecurityException.class, () ->
                loadBalanceToChild.execute(cognitoSub, studentId, new BigDecimal("50.00")));

        verify(walletRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenStudentIsNotChildOfTutor() {
        tutor.setHijos(Set.of()); // Empty children
        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.of(parentUser));
        when(tutorRepository.findByUsuarioId(parentUser.getId())).thenReturn(Optional.of(tutor));

        assertThrows(SecurityException.class, () ->
                loadBalanceToChild.execute(cognitoSub, studentId, new BigDecimal("50.00")));

        verify(walletRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenWalletNotFound() {
        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.of(parentUser));
        when(tutorRepository.findByUsuarioId(parentUser.getId())).thenReturn(Optional.of(tutor));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                loadBalanceToChild.execute(cognitoSub, studentId, new BigDecimal("50.00")));

        verify(walletRepository, never()).save(any());
    }
}
