package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.infrastructure.web.LoadStockPredictionDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoadStockPredictionDTOTest {

    @Test
    void builderAndGettersShouldWork() {
        LoadStockPredictionDTO dto = whenBuildingDto();

        thenDtoHasExpectedValues(dto);
    }

    @Test
    void settersShouldWork() {
        LoadStockPredictionDTO dto = LoadStockPredictionDTO.builder().build();
        
        whenSettingDtoValues(dto);

        thenDtoHasExpectedValues(dto);
    }

    private LoadStockPredictionDTO whenBuildingDto() {
        return LoadStockPredictionDTO.builder()
                .nombre("Nombre")
                .descripcion("Descripcion")
                .peso("100g")
                .contieneAzucar("si")
                .contieneMani("no")
                .contieneLactosa("si")
                .contieneTacc("no")
                .build();
    }

    private void whenSettingDtoValues(LoadStockPredictionDTO dto) {
        dto.setNombre("Nombre");
        dto.setDescripcion("Descripcion");
        dto.setPeso("100g");
        dto.setContieneAzucar("si");
        dto.setContieneMani("no");
        dto.setContieneLactosa("si");
        dto.setContieneTacc("no");
    }

    private void thenDtoHasExpectedValues(LoadStockPredictionDTO dto) {
        assertEquals("Nombre", dto.getNombre());
        assertEquals("Descripcion", dto.getDescripcion());
        assertEquals("100g", dto.getPeso());
        assertEquals("si", dto.getContieneAzucar());
        assertEquals("no", dto.getContieneMani());
        assertEquals("si", dto.getContieneLactosa());
        assertEquals("no", dto.getContieneTacc());
    }
}
