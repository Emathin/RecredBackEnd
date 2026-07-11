package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.infrastructure.web.AlumnoDTO;
import com.recred.backend.infrastructure.web.ClasificacionSaludDTO;
import com.recred.backend.infrastructure.web.ProductoDTO;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlumnoProductoDTOTest {

    private final UUID STUDENT_ID = UUID.randomUUID();
    private final UUID PRODUCT_ID = UUID.randomUUID();
    private final UUID CLASSIFICATION_ID_1 = UUID.randomUUID();
    private final UUID CLASSIFICATION_ID_2 = UUID.randomUUID();
    private final String SIN_TACC = "Sin TACC";
    private final String BAJO_SODIO = "Bajo sodio";
    private final String VEGANO = "Vegano";
    private final String ENSALADA = "Ensalada";

    @Test
    void alumnoDtoShouldExposeAndUpdateHealthRestrictions() {
        ClasificacionSaludDTO initial = new ClasificacionSaludDTO(CLASSIFICATION_ID_1, SIN_TACC);
        ClasificacionSaludDTO updated = new ClasificacionSaludDTO(CLASSIFICATION_ID_2, BAJO_SODIO);
        AlumnoDTO dto = new AlumnoDTO(STUDENT_ID, Set.of(initial));

        thenAlumnoDtoHasInitialRestrictions(dto, initial);

        whenUpdatingRestrictions(dto, updated);

        thenAlumnoDtoHasUpdatedRestrictions(dto, updated);
    }

    @Test
    void productoDtoShouldExposeProductValues() {
        ClasificacionSaludDTO classification = new ClasificacionSaludDTO(
                CLASSIFICATION_ID_1,
                VEGANO
        );
        ProductoDTO dto = new ProductoDTO(
                PRODUCT_ID,
                ENSALADA,
                Set.of(classification)
        );

        thenProductoDtoExposesValues(dto, classification);
    }

    private void whenUpdatingRestrictions(AlumnoDTO dto, ClasificacionSaludDTO updated) {
        dto.actualizarRestricciones(Set.of(updated));
    }

    private void thenAlumnoDtoHasInitialRestrictions(AlumnoDTO dto, ClasificacionSaludDTO initial) {
        assertEquals(STUDENT_ID, dto.getId());
        assertEquals(Set.of(initial), dto.getRestriccionesSalud());
    }

    private void thenAlumnoDtoHasUpdatedRestrictions(AlumnoDTO dto, ClasificacionSaludDTO updated) {
        assertEquals(Set.of(updated), dto.getRestriccionesSalud());
    }

    private void thenProductoDtoExposesValues(ProductoDTO dto, ClasificacionSaludDTO clazz) {
        assertEquals(PRODUCT_ID, dto.getId());
        assertEquals(ENSALADA, dto.getNombre());
        assertEquals(Set.of(clazz), dto.getClasificacionesSalud());
    }
}
