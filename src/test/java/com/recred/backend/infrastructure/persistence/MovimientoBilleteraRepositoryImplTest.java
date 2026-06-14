package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.TipoMovimientoBilletera;
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
}
