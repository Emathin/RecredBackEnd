package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.BudgetCategoryRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BudgetCategoryRuleEntityTest {

    @Test
    void toDomain_MapeaTodosLosCamposCorrectamente() {

        BudgetCategoryRuleEntity entity = new BudgetCategoryRuleEntity();
        entity.setId(UUID.randomUUID());
        entity.setPercentageLimit(new BigDecimal("15.50"));
        entity.setCalculatedLimitAmount(new BigDecimal("1550.00"));
        entity.setActive(true);

        ProductCategoryEntity categoriaEntity = new ProductCategoryEntity();
        categoriaEntity.setId(UUID.randomUUID());
        entity.setCategory(categoriaEntity);

        BudgetCategoryRule domain = entity.toDomain();

        assertEquals(entity.getId(), domain.getId());
        assertEquals(entity.getPercentageLimit(), domain.getLimitAverage());
        assertEquals(entity.getCalculatedLimitAmount(), domain.getCalculatedLimitAmount());
        assertTrue(domain.isActive());

        assertNotNull(domain.getCategory());
        assertEquals(categoriaEntity.getId(), domain.getCategory().getId());
    }

    @Test
    void toDomain_ConCategoriaNulaYActivoNulo_MapeaSeguro() {
        BudgetCategoryRuleEntity entity = new BudgetCategoryRuleEntity();
        entity.setId(UUID.randomUUID());
        entity.setActive(null);
        entity.setCategory(null);

        BudgetCategoryRule domain = entity.toDomain();

        assertFalse(domain.isActive());

        assertNull(domain.getCategory());
    }
}