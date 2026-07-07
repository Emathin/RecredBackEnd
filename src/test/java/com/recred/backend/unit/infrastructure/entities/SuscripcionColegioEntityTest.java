package com.recred.backend.unit.infrastructure.entities;

import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.domain.models.SuscripcionColegio;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.SuscripcionColegioEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SuscripcionColegioEntityTest {

    @Test
    void toDomain_ShouldMapSubscriptionFields() {
        UUID subscriptionId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        LocalDateTime startDate = LocalDateTime.of(2026, 7, 5, 10, 0);
        SuscripcionColegioEntity entity = givenEntity(subscriptionId, schoolId, startDate);

        SuscripcionColegio result = whenMappingToDomain(entity);

        assertEquals(subscriptionId, result.getId());
        assertEquals(schoolId, result.getColegioId());
        assertEquals(new BigDecimal("20.00"), result.getPrecio());
        assertEquals("USD", result.getMoneda());
        assertEquals(EstadoSuscripcion.ACTIVA, result.getEstado());
        assertEquals(startDate, result.getFechaInicio());
        assertEquals(startDate.plusMonths(1), result.getFechaVencimiento());
        assertEquals(startDate.plusMonths(2), result.getFechaGraciaHasta());
    }

    private SuscripcionColegioEntity givenEntity(
            UUID subscriptionId,
            UUID schoolId,
            LocalDateTime startDate
    ) {
        SuscripcionColegioEntity entity = new SuscripcionColegioEntity();
        entity.setId(subscriptionId);
        entity.setColegioEntity(schoolEntity(schoolId));
        entity.setPrecio(new BigDecimal("20.00"));
        entity.setMoneda("USD");
        entity.setEstado(EstadoSuscripcion.ACTIVA);
        entity.setFechaInicio(startDate);
        entity.setFechaVencimiento(startDate.plusMonths(1));
        entity.setFechaGraciaHasta(startDate.plusMonths(2));
        return entity;
    }

    private SuscripcionColegio whenMappingToDomain(SuscripcionColegioEntity entity) {
        return entity.toDomain();
    }

    private ColegioEntity schoolEntity(UUID schoolId) {
        ColegioEntity entity = new ColegioEntity();
        entity.setId(schoolId);
        return entity;
    }
}
