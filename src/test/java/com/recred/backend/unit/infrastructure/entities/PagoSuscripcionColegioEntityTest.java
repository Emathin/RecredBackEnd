package com.recred.backend.unit.infrastructure.entities;

import com.recred.backend.domain.models.EstadoPagoExterno;
import com.recred.backend.domain.models.PagoSuscripcionColegio;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.PagoSuscripcionColegioEntity;
import com.recred.backend.infrastructure.entities.SuscripcionColegioEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PagoSuscripcionColegioEntityTest {

    @Test
    void toDomain_ShouldMapPaymentFields() {
        UUID paymentId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        LocalDateTime approvedAt = LocalDateTime.of(2026, 7, 5, 10, 0);
        PagoSuscripcionColegioEntity entity = givenEntity(
                paymentId,
                schoolId,
                subscriptionId,
                approvedAt
        );

        PagoSuscripcionColegio result = whenMappingToDomain(entity);

        assertEquals(paymentId, result.getId());
        assertEquals("mp-school", result.getMercadoPagoPaymentId());
        assertEquals(schoolId, result.getColegioId());
        assertEquals(subscriptionId, result.getSuscripcionId());
        assertEquals(new BigDecimal("20.00"), result.getMonto());
        assertEquals("USD", result.getMoneda());
        assertEquals(EstadoPagoExterno.APROBADO, result.getEstado());
        assertEquals(approvedAt, result.getAprobadoEn());
    }

    private PagoSuscripcionColegioEntity givenEntity(
            UUID paymentId,
            UUID schoolId,
            UUID subscriptionId,
            LocalDateTime approvedAt
    ) {
        PagoSuscripcionColegioEntity entity = new PagoSuscripcionColegioEntity();
        entity.setId(paymentId);
        entity.setMercadoPagoPaymentId("mp-school");
        entity.setColegioEntity(schoolEntity(schoolId));
        entity.setSuscripcionEntity(subscriptionEntity(subscriptionId));
        entity.setMonto(new BigDecimal("20.00"));
        entity.setMoneda("USD");
        entity.setEstado(EstadoPagoExterno.APROBADO);
        entity.setAprobadoEn(approvedAt);
        return entity;
    }

    private PagoSuscripcionColegio whenMappingToDomain(PagoSuscripcionColegioEntity entity) {
        return entity.toDomain();
    }

    private ColegioEntity schoolEntity(UUID schoolId) {
        ColegioEntity entity = new ColegioEntity();
        entity.setId(schoolId);
        return entity;
    }

    private SuscripcionColegioEntity subscriptionEntity(UUID subscriptionId) {
        SuscripcionColegioEntity entity = new SuscripcionColegioEntity();
        entity.setId(subscriptionId);
        return entity;
    }
}
