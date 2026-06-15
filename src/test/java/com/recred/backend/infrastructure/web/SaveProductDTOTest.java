package com.recred.backend.infrastructure.web;

import com.recred.backend.application.SaveProductCommand;
import com.recred.backend.domain.models.TipoManejoInventario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

        assertEquals(dto.nombre(), command.nombre());
        assertEquals(dto.descripcion(), command.descripcion());
        assertEquals(dto.precio(), command.precio());
        assertEquals(dto.peso(), command.peso());
        assertEquals(dto.requierePreparacion(), command.requierePreparacion());
        assertEquals(dto.categoriaId(), command.categoriaId());
        assertEquals(dto.nuevaCategoriaNombre(), command.nuevaCategoriaNombre());
        assertEquals(dto.buffetId(), command.buffetId());
        assertEquals(dto.stockActual(), command.stockActual());
        assertEquals(TipoManejoInventario.STOCK_EXACTO, command.tipoManejoInventario());
        assertEquals(dto.clasificacionesSaludIds(), command.clasificacionesSaludIds());
        assertEquals(dto.tiposIds(), command.tiposIds());
    }

    @Test
    void toCommand_NoDebeForzarInventario_CuandoNoVieneStock() {
        SaveProductDTO dto = new SaveProductDTO(
                "Nombre", "Desc", BigDecimal.TEN, BigDecimal.ONE, true,
                UUID.randomUUID(), null, UUID.randomUUID(), null, Set.of(), Set.of()
        );

        SaveProductCommand command = dto.toCommand();

        assertNull(command.tipoManejoInventario());
    }
}
