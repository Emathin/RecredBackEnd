package com.recred.backend.domain.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StudentTest {

    private Student student;
    private UUID clasificacionId;
    private HealthClassification clasificacion;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(UUID.randomUUID());

        clasificacionId = UUID.randomUUID();
        clasificacion = new HealthClassification();
        clasificacion.setId(clasificacionId);
        clasificacion.setDescription("Celíaco");
    }

    @Test
    void agregarRestriccionSalud_NuevaRestriccion_DeberiaAgregarse() {
        student.addHealthRestriction(clasificacion);

        assertEquals(1, student.getHealthRestrictions().size());
        assertTrue(student.getHealthRestrictions().contains(clasificacion));
    }

    @Test
    void agregarRestriccionSalud_RestriccionDuplicada_NoDeberiaAgregarse() {
        student.addHealthRestriction(clasificacion);

        student.addHealthRestriction(clasificacion);

        HealthClassification clasificacionDuplicada = new HealthClassification();
        clasificacionDuplicada.setId(clasificacionId);
        clasificacionDuplicada.setDescription("Celíaco Modificado");

        student.addHealthRestriction(clasificacionDuplicada);

        assertEquals(1, student.getHealthRestrictions().size());
    }

    @Test
    void addHealthRestriction_Nulo_NoDeberiaHacerNada() {
        student.addHealthRestriction(null);

        assertEquals(0, student.getHealthRestrictions().size());
    }

    @Test
    void removerRestriccionSalud_RestriccionExistente_DeberiaRemoverse() {
        student.addHealthRestriction(clasificacion);
        assertEquals(1, student.getHealthRestrictions().size());

        student.removeHealthRestriction(clasificacionId);

        assertEquals(0, student.getHealthRestrictions().size());
    }

    @Test
    void removerRestriccionSalud_RestriccionNoExistente_NoDeberiaHacerNada() {
        student.addHealthRestriction(clasificacion);

        student.removeHealthRestriction(UUID.randomUUID());

        assertEquals(1, student.getHealthRestrictions().size());
    }
}