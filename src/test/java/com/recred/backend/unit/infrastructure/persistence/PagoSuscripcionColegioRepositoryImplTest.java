package com.recred.backend.unit.infrastructure.persistence;

import com.recred.backend.domain.models.EstadoPagoExterno;
import com.recred.backend.domain.models.PagoSuscripcionColegio;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.PagoSuscripcionColegioEntity;
import com.recred.backend.infrastructure.persistence.PagoSuscripcionColegioRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataPagoSuscripcionColegioRepository;
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

class PagoSuscripcionColegioRepositoryImplTest {

    private final SpringDataPagoSuscripcionColegioRepository springDataRepository =
            mock(SpringDataPagoSuscripcionColegioRepository.class);
    private final PagoSuscripcionColegioRepositoryImpl repository =
            new PagoSuscripcionColegioRepositoryImpl(springDataRepository);

    @Test
    void findByMercadoPagoPaymentId_ShouldReturnDomainPayment() {
        PagoSuscripcionColegioEntity entity = entity(UUID.randomUUID(), "mp-school");
        givenPayment("mp-school", entity);

        Optional<PagoSuscripcionColegio> result = whenFindingPayment("mp-school");

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
        verify(springDataRepository).findByMercadoPagoPaymentId("mp-school");
    }

    @Test
    void save_ShouldPersistPaymentWithSchoolReference() {
        UUID paymentId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        PagoSuscripcionColegio payment = payment(schoolId, "mp-school");
        givenSavedEntity(entity(paymentId, "mp-school"));

        PagoSuscripcionColegio result = whenSaving(payment);

        PagoSuscripcionColegioEntity savedEntity = capturedSavedEntity();
        assertEquals(paymentId, result.getId());
        assertEquals(schoolId, savedEntity.getColegioEntity().getId());
        assertEquals(new BigDecimal("20.00"), savedEntity.getMonto());
        assertEquals(EstadoPagoExterno.PENDIENTE, savedEntity.getEstado());
    }

    private void givenPayment(String paymentId, PagoSuscripcionColegioEntity entity) {
        when(springDataRepository.findByMercadoPagoPaymentId(paymentId))
                .thenReturn(Optional.of(entity));
    }

    private void givenSavedEntity(PagoSuscripcionColegioEntity entity) {
        when(springDataRepository.save(any(PagoSuscripcionColegioEntity.class)))
                .thenReturn(entity);
    }

    private Optional<PagoSuscripcionColegio> whenFindingPayment(String paymentId) {
        return repository.findByMercadoPagoPaymentId(paymentId);
    }

    private PagoSuscripcionColegio whenSaving(PagoSuscripcionColegio payment) {
        return repository.save(payment);
    }

    private PagoSuscripcionColegioEntity capturedSavedEntity() {
        ArgumentCaptor<PagoSuscripcionColegioEntity> captor =
                ArgumentCaptor.forClass(PagoSuscripcionColegioEntity.class);
        verify(springDataRepository).save(captor.capture());
        return captor.getValue();
    }

    private PagoSuscripcionColegio payment(UUID schoolId, String paymentId) {
        PagoSuscripcionColegio payment = new PagoSuscripcionColegio();
        payment.setColegioId(schoolId);
        payment.setMercadoPagoPaymentId(paymentId);
        payment.setMonto(new BigDecimal("20.00"));
        payment.setMoneda("USD");
        payment.setEstado(EstadoPagoExterno.PENDIENTE);
        return payment;
    }

    private PagoSuscripcionColegioEntity entity(UUID id, String paymentId) {
        PagoSuscripcionColegioEntity entity = new PagoSuscripcionColegioEntity();
        entity.setId(id);
        entity.setMercadoPagoPaymentId(paymentId);
        entity.setColegioEntity(schoolEntity(UUID.randomUUID()));
        entity.setMonto(new BigDecimal("20.00"));
        entity.setMoneda("USD");
        entity.setEstado(EstadoPagoExterno.PENDIENTE);
        return entity;
    }

    private ColegioEntity schoolEntity(UUID schoolId) {
        ColegioEntity entity = new ColegioEntity();
        entity.setId(schoolId);
        return entity;
    }
}
