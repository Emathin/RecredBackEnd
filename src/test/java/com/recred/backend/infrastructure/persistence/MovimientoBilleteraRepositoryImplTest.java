package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.models.WalletMovement;
import com.recred.backend.domain.models.WalletMovementDirection;
import com.recred.backend.infrastructure.entities.BilleteraEntity;
import com.recred.backend.infrastructure.entities.MovimientoBilleteraEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimientoBilleteraRepositoryImplTest {

    @Mock
    private SpringDataMovimientoBilleteraRepository springDataMovimientoBilleteraRepository;

    @Mock
    private SpringDataWalletRepository springDataWalletRepository;

    @Mock
    private SpringDataUsuarioRepository springDataUsuarioRepository;

    private MovimientoBilleteraRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new MovimientoBilleteraRepositoryImpl(
                springDataMovimientoBilleteraRepository,
                springDataWalletRepository,
                springDataUsuarioRepository
        );
    }

    @Test
    void registrarMovimientoDeberiaCrearYGuardarEntidadCorrectamente() {
        UUID walletId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        BigDecimal monto = new BigDecimal("100.50");
        String descripcion = "Test movimiento";
        TipoMovimientoBilletera tipo = TipoMovimientoBilletera.CARGA;

        BilleteraEntity walletProxy = new BilleteraEntity();
        walletProxy.setId(walletId);

        UsuarioEntity usuarioProxy = new UsuarioEntity();
        usuarioProxy.setId(usuarioId);

        when(springDataWalletRepository.getReferenceById(walletId)).thenReturn(walletProxy);
        when(springDataUsuarioRepository.getReferenceById(usuarioId)).thenReturn(usuarioProxy);

        repository.registrarMovimiento(walletId, tipo, monto, descripcion, usuarioId);

        ArgumentCaptor<MovimientoBilleteraEntity> captor = ArgumentCaptor.forClass(MovimientoBilleteraEntity.class);
        verify(springDataMovimientoBilleteraRepository).save(captor.capture());

        MovimientoBilleteraEntity savedEntity = captor.getValue();
        assertEquals(walletProxy, savedEntity.getBilleteraEntity());
        assertEquals(tipo, savedEntity.getTipo());
        assertEquals(monto, savedEntity.getMonto());
        assertEquals(descripcion, savedEntity.getDescripcion());
        assertEquals(usuarioProxy, savedEntity.getUsuarioEntity());
    }

    @Test
    void findByStudentIdAndDateRange_ShouldMapAmountAndDirection() {
        UUID studentId = UUID.randomUUID();
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        MovimientoBilleteraEntity entrada = movement(
                TipoMovimientoBilletera.CARGA,
                new BigDecimal("100.00")
        );
        MovimientoBilleteraEntity salidaPorTipo = movement(
                TipoMovimientoBilletera.COMPRA,
                new BigDecimal("25.00")
        );
        MovimientoBilleteraEntity salidaPorMonto = movement(
                TipoMovimientoBilletera.AJUSTE,
                new BigDecimal("-10.00")
        );

        when(springDataMovimientoBilleteraRepository.findByStudentIdAndDateRange(
                studentId,
                from,
                to
        )).thenReturn(List.of(entrada, salidaPorTipo, salidaPorMonto));

        List<WalletMovement> result = repository.findByStudentIdAndDateRange(
                studentId,
                from,
                to
        );

        assertEquals(WalletMovementDirection.ENTRADA, result.get(0).direction());
        assertEquals(WalletMovementDirection.SALIDA, result.get(1).direction());
        assertEquals(WalletMovementDirection.SALIDA, result.get(2).direction());
        assertEquals(new BigDecimal("10.00"), result.get(2).amount());
    }

    @Test
    void findByStudentIdAndDateRange_WhenAmountIsNull_ShouldMapZeroAmount() {
        UUID studentId = UUID.randomUUID();
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        MovimientoBilleteraEntity movement = movement(TipoMovimientoBilletera.AJUSTE, null);

        when(springDataMovimientoBilleteraRepository.findByStudentIdAndDateRange(
                studentId,
                from,
                to
        )).thenReturn(List.of(movement));

        List<WalletMovement> result = repository.findByStudentIdAndDateRange(
                studentId,
                from,
                to
        );

        assertEquals(BigDecimal.ZERO, result.get(0).amount());
        assertEquals(WalletMovementDirection.ENTRADA, result.get(0).direction());
    }

    @Test
    void registrarMovimientoSistema_WhenSystemUserExists_ShouldUseIt() {
        UUID walletId = UUID.randomUUID();
        UUID systemUserId = UUID.randomUUID();
        BilleteraEntity wallet = new BilleteraEntity();
        UsuarioEntity systemUser = new UsuarioEntity();
        systemUser.setId(systemUserId);

        when(springDataUsuarioRepository.findByIdCognito("system"))
                .thenReturn(Optional.of(systemUser));
        when(springDataWalletRepository.getReferenceById(walletId)).thenReturn(wallet);
        when(springDataUsuarioRepository.getReferenceById(systemUserId))
                .thenReturn(systemUser);

        repository.registrarMovimientoSistema(
                walletId,
                TipoMovimientoBilletera.CARGA,
                new BigDecimal("50.00"),
                "Carga sistema"
        );

        ArgumentCaptor<MovimientoBilleteraEntity> captor =
                ArgumentCaptor.forClass(MovimientoBilleteraEntity.class);
        verify(springDataMovimientoBilleteraRepository).save(captor.capture());
        assertEquals(systemUser, captor.getValue().getUsuarioEntity());
    }

    @Test
    void registrarMovimientoSistema_WhenSystemUserDoesNotExist_ShouldCreateIt() {
        UUID walletId = UUID.randomUUID();
        UUID createdUserId = UUID.randomUUID();
        BilleteraEntity wallet = new BilleteraEntity();

        when(springDataUsuarioRepository.findByIdCognito("system"))
                .thenReturn(Optional.empty());
        when(springDataUsuarioRepository.save(any(UsuarioEntity.class)))
                .thenAnswer(invocation -> {
                    UsuarioEntity user = invocation.getArgument(0);
                    user.setId(createdUserId);
                    return user;
                });
        when(springDataWalletRepository.getReferenceById(walletId)).thenReturn(wallet);
        when(springDataUsuarioRepository.getReferenceById(createdUserId))
                .thenAnswer(invocation -> {
                    UsuarioEntity user = new UsuarioEntity();
                    user.setId(createdUserId);
                    return user;
                });

        repository.registrarMovimientoSistema(
                walletId,
                TipoMovimientoBilletera.CARGA,
                new BigDecimal("50.00"),
                "Carga sistema"
        );

        ArgumentCaptor<UsuarioEntity> userCaptor =
                ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(springDataUsuarioRepository).save(userCaptor.capture());
        UsuarioEntity user = userCaptor.getValue();
        assertEquals("system", user.getIdCognito());
        assertEquals("system@recred.local", user.getEmail());
        assertEquals(RolUsuario.ADMIN, user.getRol());
    }

    private MovimientoBilleteraEntity movement(
            TipoMovimientoBilletera type,
            BigDecimal amount
    ) {
        MovimientoBilleteraEntity entity = new MovimientoBilleteraEntity();
        entity.setId(UUID.randomUUID());
        entity.setTipo(type);
        entity.setMonto(amount);
        entity.setDescripcion("Movimiento");
        entity.setCreadoEn(LocalDateTime.now());
        return entity;
    }
}
