package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.SuggestedPromotionResponse;
import com.recred.backend.infrastructure.web.SuggestedPromotionDTO;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SuggestedPromotionDTOTest {

    @Test
    void accessorsShouldExposeValues() {
        SuggestedPromotionDTO dto = new SuggestedPromotionDTO();
        dto.setName("Combo frio");
        dto.setDiscountPercentage(new BigDecimal("12.50"));
        dto.setTargetCategories(List.of("Bebidas"));
        dto.setImagen("combo.png");
        dto.setProductIds(List.of("p1", "p2"));

        assertEquals("Combo frio", dto.getName());
        assertEquals(new BigDecimal("12.50"), dto.getDiscountPercentage());
        assertEquals(List.of("Bebidas"), dto.getTargetCategories());
        assertEquals("combo.png", dto.getImagen());
        assertEquals(List.of("p1", "p2"), dto.getProductIds());
    }

    @Test
    void fromApplicationShouldMapResponseAndAcceptNull() {
        SuggestedPromotionDTO dto = SuggestedPromotionDTO.fromApplication(
                new SuggestedPromotionResponse(
                        "Promo",
                        new BigDecimal("20.00"),
                        List.of("Snacks"),
                        "promo.png",
                        List.of("a")
                )
        );

        assertEquals("Promo", dto.getName());
        assertEquals(new BigDecimal("20.00"), dto.getDiscountPercentage());
        assertEquals(List.of("Snacks"), dto.getTargetCategories());
        assertEquals("promo.png", dto.getImagen());
        assertEquals(List.of("a"), dto.getProductIds());
        assertNull(SuggestedPromotionDTO.fromApplication(null));
    }
}
