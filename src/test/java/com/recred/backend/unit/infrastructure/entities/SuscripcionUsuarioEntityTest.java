package com.recred.backend.unit.infrastructure.entities;

import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.SuscripcionUsuario;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.infrastructure.entities.SuscripcionUsuarioEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SuscripcionUsuarioEntityTest {

    @Test
    void toDomain_ShouldMapSubscriptionFields() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime startDate = LocalDateTime.of(2026, 7, 5, 10, 0);
        SuscripcionUsuarioEntity entity = givenEntity(subscriptionId, userId, startDate);

        SuscripcionUsuario result = whenMappingToDomain(entity);

        assertEquals(subscriptionId, result.getId());
        assertEquals(userId, result.getUsuarioId());
        assertEquals(TipoPlan.AVANZADO, result.getPlan());
        assertEquals(PeriodoSuscripcion.ANUAL, result.getPeriodo());
        assertEquals(new BigDecimal("97920.00"), result.getPrecio());
        assertEquals(EstadoSuscripcion.ACTIVA, result.getEstado());
        assertEquals(startDate, result.getFechaInicio());
        assertEquals(startDate.plusYears(1), result.getFechaVencimiento());
        assertEquals(startDate.plusMonths(11), result.getUltimoAvisoVencimientoEn());
    }

    private SuscripcionUsuarioEntity givenEntity(
            UUID subscriptionId,
            UUID userId,
            LocalDateTime startDate
    ) {
        SuscripcionUsuarioEntity entity = new SuscripcionUsuarioEntity();
        entity.setId(subscriptionId);
        entity.setUsuarioEntity(userEntity(userId));
        entity.setPlan(TipoPlan.AVANZADO);
        entity.setPeriodo(PeriodoSuscripcion.ANUAL);
        entity.setPrecio(new BigDecimal("97920.00"));
        entity.setEstado(EstadoSuscripcion.ACTIVA);
        entity.setFechaInicio(startDate);
        entity.setFechaVencimiento(startDate.plusYears(1));
        entity.setUltimoAvisoVencimientoEn(startDate.plusMonths(11));
        return entity;
    }

    private SuscripcionUsuario whenMappingToDomain(SuscripcionUsuarioEntity entity) {
        return entity.toDomain();
    }

    private UsuarioEntity userEntity(UUID userId) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setId(userId);
        return entity;
    }
}
