package com.recred.backend.web;

import com.recred.backend.infrastructure.web.LoadStockPredictionDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoadStockPredictionDTOTest {

    @Test
    void testBuilderAndGetters() {
        LoadStockPredictionDTO dto = LoadStockPredictionDTO.builder()
                .nombre("Nombre")
                .descripcion("Descripcion")
                .peso("100g")
                .contieneAzucar("si")
                .contieneMani("no")
                .contieneLactosa("si")
                .contieneTacc("no")
                .build();

        assertEquals("Nombre", dto.getNombre());
        assertEquals("Descripcion", dto.getDescripcion());
        assertEquals("100g", dto.getPeso());
        assertEquals("si", dto.getContieneAzucar());
        assertEquals("no", dto.getContieneMani());
        assertEquals("si", dto.getContieneLactosa());
        assertEquals("no", dto.getContieneTacc());
    }

    @Test
    void testSetters() {
        LoadStockPredictionDTO dto = LoadStockPredictionDTO.builder().build();
        dto.setNombre("Nombre");
        dto.setDescripcion("Descripcion");
        dto.setPeso("100g");
        dto.setContieneAzucar("si");
        dto.setContieneMani("no");
        dto.setContieneLactosa("si");
        dto.setContieneTacc("no");

        assertEquals("Nombre", dto.getNombre());
        assertEquals("Descripcion", dto.getDescripcion());
        assertEquals("100g", dto.getPeso());
        assertEquals("si", dto.getContieneAzucar());
        assertEquals("no", dto.getContieneMani());
        assertEquals("si", dto.getContieneLactosa());
        assertEquals("no", dto.getContieneTacc());
    }
}
