package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TutorTest {

    @Test
    void addHijo_ShouldLinkTutorAndStudent() {
        Tutor tutor = new Tutor();
        Alumno alumno = new Alumno();

        tutor.addHijo(alumno);

        assertTrue(tutor.getHijos().contains(alumno));
        assertTrue(alumno.getTutores().contains(tutor));
    }

    @Test
    void addHijo_WhenStudentIsNull_ShouldDoNothing() {
        Tutor tutor = new Tutor();

        tutor.addHijo(null);

        assertTrue(tutor.getHijos().isEmpty());
    }

    @Test
    void removeHijo_ShouldUnlinkTutorAndStudent() {
        Tutor tutor = new Tutor();
        Alumno alumno = new Alumno();
        tutor.addHijo(alumno);

        tutor.removeHijo(alumno);

        assertFalse(tutor.getHijos().contains(alumno));
        assertFalse(alumno.getTutores().contains(tutor));
    }

    @Test
    void removeHijo_WhenStudentIsNull_ShouldDoNothing() {
        Tutor tutor = new Tutor();

        tutor.removeHijo(null);

        assertTrue(tutor.getHijos().isEmpty());
    }
}
