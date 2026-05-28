package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.ReglaPresupuestoCategoria;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReglaPresupuestoCategoriaEntityTest {

    @Test
    void toDomain_MapeaTodosLosCamposCorrectamente() {

        ReglaPresupuestoCategoriaEntity entity = new ReglaPresupuestoCategoriaEntity();
        entity.setId(UUID.randomUUID());
        entity.setPorcentajeLimite(new BigDecimal("15.50"));
        entity.setMontoLimiteCalculado(new BigDecimal("1550.00"));
        entity.setActivo(true);

        CategoriaProductoEntity categoriaEntity = new CategoriaProductoEntity();
        categoriaEntity.setId(UUID.randomUUID());
        entity.setCategoria(categoriaEntity);

        ReglaPresupuestoCategoria domain = entity.toDomain();

        assertEquals(entity.getId(), domain.getId());
        assertEquals(entity.getPorcentajeLimite(), domain.getPorcentajeLimite());
        assertEquals(entity.getMontoLimiteCalculado(), domain.getMontoLimiteCalculado());
        assertTrue(domain.isActivo());

        assertNotNull(domain.getCategoria());
        assertEquals(categoriaEntity.getId(), domain.getCategoria().getId());
    }

    @Test
    void toDomain_ConCategoriaNulaYActivoNulo_MapeaSeguro() {
        ReglaPresupuestoCategoriaEntity entity = new ReglaPresupuestoCategoriaEntity();
        entity.setId(UUID.randomUUID());
        entity.setActivo(null);
        entity.setCategoria(null);

        ReglaPresupuestoCategoria domain = entity.toDomain();

        assertFalse(domain.isActivo());

        assertNull(domain.getCategoria());
    }
}