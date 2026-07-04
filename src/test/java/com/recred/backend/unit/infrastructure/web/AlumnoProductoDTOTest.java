package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.infrastructure.web.AlumnoDTO;
import com.recred.backend.infrastructure.web.ClasificacionSaludDTO;
import com.recred.backend.infrastructure.web.ProductoDTO;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlumnoProductoDTOTest {

    @Test
    void alumnoDtoShouldExposeAndUpdateHealthRestrictions() {
        UUID studentId = UUID.randomUUID();
        ClasificacionSaludDTO initial = new ClasificacionSaludDTO(
                UUID.randomUUID(),
                "Sin TACC"
        );
        ClasificacionSaludDTO updated = new ClasificacionSaludDTO(
                UUID.randomUUID(),
                "Bajo sodio"
        );
        AlumnoDTO dto = new AlumnoDTO(studentId, Set.of(initial));

        assertEquals(studentId, dto.getId());
        assertEquals(Set.of(initial), dto.getRestriccionesSalud());

        dto.actualizarRestricciones(Set.of(updated));

        assertEquals(Set.of(updated), dto.getRestriccionesSalud());
    }

    @Test
    void productoDtoShouldExposeProductValues() {
        UUID productId = UUID.randomUUID();
        ClasificacionSaludDTO classification = new ClasificacionSaludDTO(
                UUID.randomUUID(),
                "Vegano"
        );
        ProductoDTO dto = new ProductoDTO(
                productId,
                "Ensalada",
                Set.of(classification)
        );

        assertEquals(productId, dto.getId());
        assertEquals("Ensalada", dto.getNombre());
        assertEquals(Set.of(classification), dto.getClasificacionesSalud());
    }
}
