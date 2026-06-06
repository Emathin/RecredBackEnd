package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Usuario;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentDTOTest {

    @Test
    void dadoUnAlumnoConUsuarioCompleto_cuandoMapeaADTO_entoncesExtraeNombreYApellido() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Martina");
        usuario.setApellido("Gómez");

        Alumno alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        alumno.setUsuario(usuario);
        alumno.setActivo(true);

        StudentDTO dto = StudentDTO.fromDomain(alumno);

        assertEquals(alumno.getId(), dto.id());
        assertEquals("Martina", dto.nombre());
        assertEquals("Gómez", dto.apellido());
        assertTrue(dto.activo());
    }

    @Test
    void dadoUnAlumnoConUsuarioNulo_cuandoMapeaADTO_entoncesAsignaValoresPorDefecto() {
        Alumno alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        alumno.setUsuario(null); // Simulamos el caso crítico
        alumno.setActivo(true);

        StudentDTO dto = StudentDTO.fromDomain(alumno);

        assertEquals(alumno.getId(), dto.id());
        assertEquals("Sin nombre", dto.nombre());
        assertEquals("Sin apellido", dto.apellido());
    }
}