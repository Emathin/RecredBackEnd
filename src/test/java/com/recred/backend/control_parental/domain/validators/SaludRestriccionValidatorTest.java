package com.recred.backend.control_parental.domain.validators;

import com.recred.backend.dominio.exceptions.RestriccionSaludException;
import com.recred.backend.dominio.models.Alumno;
import com.recred.backend.dominio.models.ClasificacionSalud;
import com.recred.backend.dominio.models.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SaludRestriccionValidatorTest {

    private SaludRestriccionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SaludRestriccionValidator();
    }

    @Test
    void validar_DebePasar_CuandoAlumnoTieneRestriccionesNulas() {
        Alumno alumno = new Alumno(UUID.randomUUID(), null);
        Producto producto = new Producto(UUID.randomUUID(), "Agua", Collections.emptySet());

        assertDoesNotThrow(() -> validator.validar(alumno, List.of(producto)));
    }

    @Test
    void validar_DebePasar_CuandoAlumnoTieneRestriccionesVacias() {
        Alumno alumno = new Alumno(UUID.randomUUID(), Collections.emptySet());
        Producto producto = new Producto(UUID.randomUUID(), "Agua", Collections.emptySet());

        assertDoesNotThrow(() -> validator.validar(alumno, List.of(producto)));
    }

    @Test
    void validar_DebePasar_CuandoProductoTieneClasificacionesNulas() {
        ClasificacionSalud restriccion = new ClasificacionSalud(UUID.randomUUID(), "Sin TACC");
        Alumno alumno = new Alumno(UUID.randomUUID(), Set.of(restriccion));
        Producto producto = new Producto(UUID.randomUUID(), "Manzana", null);

        assertDoesNotThrow(() -> validator.validar(alumno, List.of(producto)));
    }

    @Test
    void validar_DebePasar_CuandoNoHayCoincidenciaDeRestricciones() {
        ClasificacionSalud restriccionAlumno = new ClasificacionSalud(UUID.randomUUID(), "Alergia Maní");
        ClasificacionSalud restriccionProducto = new ClasificacionSalud(UUID.randomUUID(), "Sin TACC");

        Alumno alumno = new Alumno(UUID.randomUUID(), Set.of(restriccionAlumno));
        Producto producto = new Producto(UUID.randomUUID(), "Galleta", Set.of(restriccionProducto));

        assertDoesNotThrow(() -> validator.validar(alumno, List.of(producto)));
    }

    @Test
    void validar_DebeLanzarExcepcion_CuandoHayCoincidenciaDeRestricciones() {
        UUID mismaRestriccionId = UUID.randomUUID();
        ClasificacionSalud restriccion = new ClasificacionSalud(mismaRestriccionId, "Alergia Maní");

        Alumno alumno = new Alumno(UUID.randomUUID(), Set.of(restriccion));
        Producto producto = new Producto(UUID.randomUUID(), "Alfajor de Maní", Set.of(restriccion));

        RestriccionSaludException exception = assertThrows(
                RestriccionSaludException.class,
                () -> validator.validar(alumno, List.of(producto))
        );

        assertEquals("El producto 'Alfajor de Maní' contiene ingredientes restringidos.", exception.getMessage());
    }
}