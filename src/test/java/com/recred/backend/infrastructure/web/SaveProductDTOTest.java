package com.recred.backend.infrastructure.web;

import com.recred.backend.application.SaveProductCommand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaveProductDTOTest {

    @Test
    void toCommand_DeberiaMapearTodosLosCampos() {
        UUID categoriaId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Set<UUID> saludIds = Set.of(UUID.randomUUID());
        Set<UUID> tiposIds = Set.of(UUID.randomUUID());

        SaveProductDTO dto = new SaveProductDTO(
                "Nombre", "Desc", BigDecimal.TEN, BigDecimal.ONE, true,
                categoriaId, "Nueva Cat", buffetId, 10, saludIds, tiposIds
        );

        SaveProductCommand command = dto.toCommand();

        assertEquals(dto.name(), command.nombre());
        assertEquals(dto.description(), command.descripcion());
        assertEquals(dto.price(), command.precio());
        assertEquals(dto.weight(), command.peso());
        assertEquals(dto.requiresPreparation(), command.requierePreparacion());
        assertEquals(dto.categoryID(), command.categoriaId());
        assertEquals(dto.newCategoryName(), command.nuevaCategoriaNombre());
        assertEquals(dto.buffetId(), command.buffetId());
        assertEquals(dto.currentStock(), command.stockActual());
        assertEquals(dto.healthClassificationIds(), command.clasificacionesSaludIds());
        assertEquals(dto.idsTypes(), command.tiposIds());
    }
}
