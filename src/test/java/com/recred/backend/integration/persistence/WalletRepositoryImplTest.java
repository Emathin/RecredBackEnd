package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Billetera;
import com.recred.backend.infrastructure.entities.BilleteraEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletRepositoryImplTest {

    @Mock
    private SpringDataWalletRepository springDataRepository;

    private WalletRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new WalletRepositoryImpl(springDataRepository);
    }

    @Test
    void findByStudentId_ShouldMapWalletEntityToDomain() {
        UUID studentId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        BilleteraEntity entity = walletEntity(walletId);
        when(springDataRepository.findByAlumnoEntityId(studentId))
                .thenReturn(Optional.of(entity));

        Optional<Billetera> result = repository.findByStudentId(studentId);

        assertTrue(result.isPresent());
        assertEquals(walletId, result.get().getId());
        assertEquals(new BigDecimal("120.00"), result.get().getSaldoCreditos());
        assertEquals(new BigDecimal("50.00"), result.get().getLimiteCredito());
        assertEquals(new BigDecimal("10.00"), result.get().getSaldoDeudaInterno());
        assertTrue(result.get().getCreditoHabilitado());
    }

    @Test
    void findByStudentId_WhenWalletDoesNotExist_ShouldReturnEmptyOptional() {
        UUID studentId = UUID.randomUUID();
        when(springDataRepository.findByAlumnoEntityId(studentId)).thenReturn(Optional.empty());

        Optional<Billetera> result = repository.findByStudentId(studentId);

        assertTrue(result.isEmpty());
    }

    @Test
    void save_ShouldUpdateExistingEntity() {
        UUID walletId = UUID.randomUUID();
        BilleteraEntity entity = walletEntity(walletId);
        Billetera wallet = new Billetera();
        wallet.setId(walletId);
        wallet.setSaldoCreditos(new BigDecimal("75.00"));
        wallet.setLimiteCredito(new BigDecimal("25.00"));
        wallet.setSaldoDeudaInterno(new BigDecimal("5.00"));
        wallet.setCreditoHabilitado(false);

        when(springDataRepository.findById(walletId)).thenReturn(Optional.of(entity));

        repository.save(wallet);

        ArgumentCaptor<BilleteraEntity> captor = ArgumentCaptor.forClass(BilleteraEntity.class);
        verify(springDataRepository).save(captor.capture());
        BilleteraEntity saved = captor.getValue();
        assertEquals(new BigDecimal("75.00"), saved.getSaldoCreditos());
        assertEquals(new BigDecimal("25.00"), saved.getLimiteCredito());
        assertEquals(new BigDecimal("5.00"), saved.getSaldoDeudaInterno());
        assertEquals(false, saved.getCreditoHabilitado());
    }

    @Test
    void save_WhenWalletDoesNotExist_ShouldThrowException() {
        UUID walletId = UUID.randomUUID();
        Billetera wallet = new Billetera();
        wallet.setId(walletId);
        when(springDataRepository.findById(walletId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> repository.save(wallet));
    }

    @Test
    void findAlumnoIdsWithLowBalance_ShouldDelegateToSpringDataRepository() {
        BigDecimal threshold = new BigDecimal("30.00");
        UUID studentId = UUID.randomUUID();
        when(springDataRepository.findAlumnoIdsWithLowBalance(threshold))
                .thenReturn(List.of(studentId));

        List<UUID> result = repository.findAlumnoIdsWithLowBalance(threshold);

        assertEquals(List.of(studentId), result);
    }

    private BilleteraEntity walletEntity(UUID walletId) {
        BilleteraEntity entity = new BilleteraEntity();
        entity.setId(walletId);
        entity.setSaldoCreditos(new BigDecimal("120.00"));
        entity.setLimiteCredito(new BigDecimal("50.00"));
        entity.setSaldoDeudaInterno(new BigDecimal("10.00"));
        entity.setCreditoHabilitado(true);
        return entity;
    }
}
