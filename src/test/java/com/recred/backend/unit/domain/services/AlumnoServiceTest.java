package com.recred.backend.unit.domain.services;

import com.recred.backend.domain.services.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AlumnoServiceTest {

    @Test
    void implementsDomainAlumnoServiceContract() {
        assertInstanceOf(com.recred.backend.domain.AlumnoService.class, new AlumnoService());
    }
}
