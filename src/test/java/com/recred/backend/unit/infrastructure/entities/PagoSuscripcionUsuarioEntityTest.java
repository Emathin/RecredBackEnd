package com.recred.backend.unit.infrastructure.entities;

import com.recred.backend.domain.models.EstadoPagoExterno;
import com.recred.backend.domain.models.PagoSuscripcionUsuario;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.infrastructure.entities.PagoSuscripcionUsuarioEntity;
import com.recred.backend.infrastructure.entities.SuscripcionUsuarioEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PagoSuscripcionUsuarioEntityTest {

    @Test
    void toDomain_ShouldMapPaymentFields() {
        UUID paymentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        LocalDateTime approvedAt = LocalDateTime.of(2026, 7, 5, 10, 0);
        PagoSuscripcionUsuarioEntity entity = givenEntity(
                paymentId,
                userId,
                subscriptionId,
                approvedAt
        );

        PagoSuscripcionUsuario result = whenMappingToDomain(entity);

        assertEquals(paymentId, result.getId());
        assertEquals("mp-123", result.getMercadoPagoPaymentId());
        assertEquals(userId, result.getUsuarioId());
        assertEquals(subscriptionId, result.getSuscripcionId());
        assertEquals(TipoPlan.AVANZADO, result.getPlan());
        assertEquals(PeriodoSuscripcion.ANUAL, result.getPeriodo());
        assertEquals(new BigDecimal("97920.00"), result.getMonto());
        assertEquals("ARS", result.getMoneda());
        assertEquals(EstadoPagoExterno.APROBADO, result.getEstado());
        assertEquals(approvedAt, result.getAprobadoEn());
    }

    private PagoSuscripcionUsuarioEntity givenEntity(
            UUID paymentId,
            UUID userId,
            UUID subscriptionId,
            LocalDateTime approvedAt
    ) {
        PagoSuscripcionUsuarioEntity entity = new PagoSuscripcionUsuarioEntity();
        entity.setId(paymentId);
        entity.setMercadoPagoPaymentId("mp-123");
        entity.setUsuarioEntity(userEntity(userId));
        entity.setSuscripcionEntity(subscriptionEntity(subscriptionId));
        entity.setPlan(TipoPlan.AVANZADO);
        entity.setPeriodo(PeriodoSuscripcion.ANUAL);
        entity.setMonto(new BigDecimal("97920.00"));
        entity.setMoneda("ARS");
        entity.setEstado(EstadoPagoExterno.APROBADO);
        entity.setAprobadoEn(approvedAt);
        return entity;
    }

    private PagoSuscripcionUsuario whenMappingToDomain(PagoSuscripcionUsuarioEntity entity) {
        return entity.toDomain();
    }

    private UsuarioEntity userEntity(UUID userId) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setId(userId);
        return entity;
    }

    private SuscripcionUsuarioEntity subscriptionEntity(UUID subscriptionId) {
        SuscripcionUsuarioEntity entity = new SuscripcionUsuarioEntity();
        entity.setId(subscriptionId);
        return entity;
    }
}
