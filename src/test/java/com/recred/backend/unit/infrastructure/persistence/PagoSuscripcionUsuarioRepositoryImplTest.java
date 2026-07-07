package com.recred.backend.unit.infrastructure.persistence;

import com.recred.backend.domain.models.EstadoPagoExterno;
import com.recred.backend.domain.models.PagoSuscripcionUsuario;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.infrastructure.entities.PagoSuscripcionUsuarioEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.PagoSuscripcionUsuarioRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataPagoSuscripcionUsuarioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PagoSuscripcionUsuarioRepositoryImplTest {

    private final SpringDataPagoSuscripcionUsuarioRepository springDataRepository =
            mock(SpringDataPagoSuscripcionUsuarioRepository.class);
    private final PagoSuscripcionUsuarioRepositoryImpl repository =
            new PagoSuscripcionUsuarioRepositoryImpl(springDataRepository);

    @Test
    void findByMercadoPagoPaymentId_ShouldReturnDomainPayment() {
        PagoSuscripcionUsuarioEntity entity = entity(UUID.randomUUID(), "mp-123");
        givenPayment("mp-123", entity);

        Optional<PagoSuscripcionUsuario> result = whenFindingPayment("mp-123");

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
        verify(springDataRepository).findByMercadoPagoPaymentId("mp-123");
    }

    @Test
    void save_ShouldPersistPaymentWithUserReference() {
        UUID paymentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PagoSuscripcionUsuario payment = payment(userId, "mp-456");
        givenSavedEntity(entity(paymentId, "mp-456"));

        PagoSuscripcionUsuario result = whenSaving(payment);

        PagoSuscripcionUsuarioEntity savedEntity = capturedSavedEntity();
        assertEquals(paymentId, result.getId());
        assertEquals(userId, savedEntity.getUsuarioEntity().getId());
        assertEquals(TipoPlan.INTERMEDIO, savedEntity.getPlan());
        assertEquals(EstadoPagoExterno.PENDIENTE, savedEntity.getEstado());
    }

    private void givenPayment(String paymentId, PagoSuscripcionUsuarioEntity entity) {
        when(springDataRepository.findByMercadoPagoPaymentId(paymentId))
                .thenReturn(Optional.of(entity));
    }

    private void givenSavedEntity(PagoSuscripcionUsuarioEntity entity) {
        when(springDataRepository.saveAndFlush(any(PagoSuscripcionUsuarioEntity.class)))
                .thenReturn(entity);
    }

    private Optional<PagoSuscripcionUsuario> whenFindingPayment(String paymentId) {
        return repository.findByMercadoPagoPaymentId(paymentId);
    }

    private PagoSuscripcionUsuario whenSaving(PagoSuscripcionUsuario payment) {
        return repository.save(payment);
    }

    private PagoSuscripcionUsuarioEntity capturedSavedEntity() {
        ArgumentCaptor<PagoSuscripcionUsuarioEntity> captor =
                ArgumentCaptor.forClass(PagoSuscripcionUsuarioEntity.class);
        verify(springDataRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private PagoSuscripcionUsuario payment(UUID userId, String paymentId) {
        PagoSuscripcionUsuario payment = new PagoSuscripcionUsuario();
        payment.setUsuarioId(userId);
        payment.setMercadoPagoPaymentId(paymentId);
        payment.setPlan(TipoPlan.INTERMEDIO);
        payment.setPeriodo(PeriodoSuscripcion.MENSUAL);
        payment.setMonto(new BigDecimal("5500.00"));
        payment.setEstado(EstadoPagoExterno.PENDIENTE);
        return payment;
    }

    private PagoSuscripcionUsuarioEntity entity(UUID id, String paymentId) {
        PagoSuscripcionUsuarioEntity entity = new PagoSuscripcionUsuarioEntity();
        entity.setId(id);
        entity.setMercadoPagoPaymentId(paymentId);
        entity.setUsuarioEntity(userEntity(UUID.randomUUID()));
        entity.setPlan(TipoPlan.INTERMEDIO);
        entity.setPeriodo(PeriodoSuscripcion.MENSUAL);
        entity.setMonto(new BigDecimal("5500.00"));
        entity.setMoneda("ARS");
        entity.setEstado(EstadoPagoExterno.PENDIENTE);
        return entity;
    }

    private UsuarioEntity userEntity(UUID userId) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setId(userId);
        return entity;
    }
}
