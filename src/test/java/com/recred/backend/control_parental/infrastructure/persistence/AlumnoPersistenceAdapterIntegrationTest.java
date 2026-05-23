package com.recred.backend.control_parental.infrastructure.persistence;

import com.recred.backend.infraestructura.entities.ClasificacionSaludEntity;
import com.recred.backend.infraestructura.AlumnoPersistenceAdapter;
import com.recred.backend.dominio.models.Alumno;
import com.recred.backend.dominio.models.ClasificacionSalud;
import com.recred.backend.infraestructura.AlumnoMapper;
import com.recred.backend.infraestructura.entities.AlumnoEntity;
import com.recred.backend.infraestructura.entities.UsuarioEntity;
import com.recred.backend.infraestructura.entities.RolUsuario;
import com.recred.backend.dominio.AlumnoRepository;
import com.recred.backend.repositories.ClasificacionSaludRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Import({AlumnoPersistenceAdapter.class, AlumnoMapper.class}) // Importamos nuestro adaptador para probarlo
class AlumnoPersistenceAdapterIntegrationTest {

    @Autowired
    private AlumnoRepository alumnoRepository;

    @Autowired
    private ClasificacionSaludRepository saludRepository;

    @Autowired
    private AlumnoPersistenceAdapter adapter;

    @Test
    void guardar_DebePersistirRestriccionesEnTablaIntermedia() {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setNombre("Padre");
        usuario.setApellido("Test"); // <--- ¡Este era el culpable del error!
        usuario.setEmail("padre@test.com");
        usuario.setIdCognito("cognito-padre-123");
        usuario.setAutenticadoConCognito(false);
        usuario.setActivo(true);
        usuario.setRol(RolUsuario.PADRE);

        ClasificacionSaludEntity restriccion = new ClasificacionSaludEntity();
        restriccion.setDescripcion("Contiene Maní");
        restriccion = saludRepository.save(restriccion);

        AlumnoEntity alumnoEntity = new AlumnoEntity();
        alumnoEntity.setUsuario(usuario); // El CascadeType.ALL guardará al usuario automáticamente
        alumnoEntity.setActivo(true);

        alumnoEntity = alumnoRepository.save(alumnoEntity);

        Alumno alumnoDominio = new Alumno(
                alumnoEntity.getId(),
                Set.of(new ClasificacionSalud(restriccion.getId(), restriccion.getDescripcion()))
        );

        adapter.guardar(alumnoDominio);

        AlumnoEntity alumnoGuardado = alumnoRepository.findById(alumnoEntity.getId()).orElseThrow();
        assertEquals(1, alumnoGuardado.getRestriccionesSalud().size());
    }
}