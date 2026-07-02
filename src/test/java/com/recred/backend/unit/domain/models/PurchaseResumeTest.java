package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.*;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PurchaseResumeTest {

    @Test
    void constructorWithSuggestion_ShouldSetBaseFieldsAndSuggestion() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID suggestionId = UUID.randomUUID();
        LocalDateTime date = LocalDateTime.of(2026, 6, 15, 10, 0);

        PurchaseResume resume = new PurchaseResume(
                studentId,
                productId,
                "Alfajor",
                new BigDecimal("20.00"),
                date,
                suggestionId
        );

        assertEquals(studentId, resume.getIdAlumno());
        assertEquals(productId, resume.getIdProducto());
        assertEquals("Alfajor", resume.getNombreProducto());
        assertEquals(new BigDecimal("20.00"), resume.getPrecio());
        assertEquals(date, resume.getDate());
        assertEquals(suggestionId, resume.getSuggestionId());
    }
}
