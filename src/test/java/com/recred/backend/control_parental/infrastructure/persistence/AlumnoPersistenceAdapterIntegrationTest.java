package com.recred.backend.control_parental.infrastructure.persistence;

import com.recred.backend.catalogo.infrastructure.entities.ClasificacionSaludEntity;
import com.recred.backend.catalogo.infrastructure.repositories.ClasificacionSaludRepository;
import com.recred.backend.usuarios.infrastructure.entities.AlumnoEntity;
import com.recred.backend.usuarios.infrastructure.entities.UsuarioEntity;
import com.recred.backend.usuarios.domain.enums.RolUsuario;
import com.recred.backend.usuarios.infrastructure.repositories.AlumnoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(AlumnoPersistenceAdapter.class) // Importamos nuestro adaptador para probarlo
class AlumnoPersistenceAdapterIntegrationTest {

    @Autowired
    private AlumnoRepository alumnoRepository;

    @Autowired
    private ClasificacionSaludRepository saludRepository;

    @Autowired
    private AlumnoPersistenceAdapter adapter;

    @Test
    void guardar_DebePersistirRestriccionesEnTablaIntermedia() {
        // 1. Necesitamos un Usuario válido (con sus campos obligatorios)
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setNombre("Padre");
        usuario.setApellido("Test");
        usuario.setEmail("test@email.com");
        usuario.setIdCognito("cognito-123");
        usuario.setAutenticadoConCognito(false);
        usuario.setActivo(true);
        usuario.setRol(RolUsuario.PADRE); // O el Enum que tengas definido

        // 2. Creamos la restricción
        ClasificacionSaludEntity restriccion = new ClasificacionSaludEntity();
        restriccion.setDescripcion("Contiene Maní");
        restriccion = saludRepository.save(restriccion);

        // 3. Creamos el Alumno asociado al Usuario
        AlumnoEntity alumno = new AlumnoEntity();
        alumno.setUsuario(usuario); // <--- Gracias al cascade, al guardar el alumno se guarda el usuario
        alumno.setActivo(true);
        alumno.setRestriccionesSalud(Set.of(restriccion));

        // 4. Act
        adapter.guardar(alumno);

        // 5. Assert
        AlumnoEntity alumnoGuardado = alumnoRepository.findById(alumno.getId()).orElseThrow();
        assertEquals(1, alumnoGuardado.getRestriccionesSalud().size());
    }
}