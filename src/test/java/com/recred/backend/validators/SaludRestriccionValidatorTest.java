package com.recred.backend.validators;

import com.recred.backend.control_parental.domain.exceptions.RestriccionSaludException;
import com.recred.backend.control_parental.domain.validators.SaludRestriccionValidator;
import com.recred.backend.usuarios.infrastructure.entities.AlumnoEntity;
import com.recred.backend.catalogo.infrastructure.entities.ClasificacionSaludEntity;
import com.recred.backend.catalogo.infrastructure.entities.ProductoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SaludRestriccionValidatorTest {

    private SaludRestriccionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SaludRestriccionValidator();
    }

    @Test
    @DisplayName("Debe lanzar excepción si el producto contiene una restricción de salud del alumno")
    void debeLanzarExcepcionConProductoRestringido() {
        // Arrange
        ClasificacionSaludEntity celiaquia = new ClasificacionSaludEntity();
        celiaquia.setDescripcion("Sin TACC");

        AlumnoEntity alumnoEntity = new AlumnoEntity();
        alumnoEntity.setRestriccionesSalud(Set.of(celiaquia));

        ProductoEntity alfajor = new ProductoEntity();
        alfajor.setNombre("Alfajor de Chocolate");
        alfajor.setClasificacionesSalud(Set.of(celiaquia)); // El producto "contiene" el alérgeno

        // Act & Assert
        assertThrows(RestriccionSaludException.class, () -> {
            validator.validar(alumnoEntity, List.of(alfajor));
        });
    }

    @Test
    @DisplayName("Debe pasar la validación si el producto es apto para el alumno")
    void debePasarValidacionConProductoApto() {
        // Arrange
        ClasificacionSaludEntity celiaquia = new ClasificacionSaludEntity();
        celiaquia.setDescripcion("Sin TACC");

        AlumnoEntity alumnoEntity = new AlumnoEntity();
        alumnoEntity.setRestriccionesSalud(Set.of(celiaquia));

        ProductoEntity manzana = new ProductoEntity();
        manzana.setNombre("Manzana");
        // La manzana no tiene clasificaciones de salud cruzadas

        // Act & Assert
        assertDoesNotThrow(() -> validator.validar(alumnoEntity, List.of(manzana)));
    }
}