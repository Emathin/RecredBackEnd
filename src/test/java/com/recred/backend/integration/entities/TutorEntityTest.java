package com.recred.backend.integration.entities;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.infrastructure.entities.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TutorEntityTest {

    @Test
    void addAndRemoveHijoShouldKeepBidirectionalRelation() {
        TutorEntity tutor = new TutorEntity();
        AlumnoEntity alumno = new AlumnoEntity();

        tutor.addHijo(alumno);

        assertTrue(tutor.getHijos().contains(alumno));
        assertTrue(alumno.getTutores().contains(tutor));

        tutor.removeHijo(alumno);

        assertTrue(tutor.getHijos().isEmpty());
        assertTrue(alumno.getTutores().isEmpty());
    }

    @Test
    void toDomainShouldMapTutorAndChildren() {
        UUID tutorId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 15, 11, 0);
        TutorEntity entity = new TutorEntity();
        entity.setId(tutorId);
        entity.setActiva(false);
        entity.setCreadoEn(createdAt);
        entity.setActualizadoEn(updatedAt);
        entity.setUsuario(userEntity());
        entity.addHijo(childEntity(childId));

        Tutor result = entity.toDomain();

        assertEquals(tutorId, result.getId());
        assertEquals(false, result.getActiva());
        assertEquals(createdAt, result.getCreadoEn());
        assertEquals(updatedAt, result.getActualizadoEn());
        assertEquals("Tutor", result.getUsuario().getNombre());
        assertEquals(1, result.getHijos().size());
        assertEquals(childId, result.getHijos().iterator().next().getId());
    }

    @Test
    void fromDomainShouldMapChildrenWithIdAndIgnoreNullIds() {
        UUID tutorId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        tutor.setActiva(true);
        tutor.setHijos(Set.of(alumno(childId), alumno(null)));

        TutorEntity result = TutorEntity.fromDomain(tutor);

        assertEquals(tutorId, result.getId());
        assertEquals(true, result.getActiva());
        assertEquals(1, result.getHijos().size());
        AlumnoEntity child = result.getHijos().iterator().next();
        assertEquals(childId, child.getId());
        assertTrue(child.getTutores().contains(result));
    }

    @Test
    void fromDomainShouldReturnNullWhenTutorIsNull() {
        assertNull(TutorEntity.fromDomain(null));
    }

    private UsuarioEntity userEntity() {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setId(UUID.randomUUID());
        entity.setIdCognito("sub");
        entity.setNombre("Tutor");
        entity.setApellido("Responsable");
        entity.setEmail("tutor@example.com");
        entity.setRol(RolUsuario.PADRE);
        entity.setActivo(true);
        entity.setAutenticadoConCognito(true);
        return entity;
    }

    private AlumnoEntity childEntity(UUID id) {
        AlumnoEntity entity = new AlumnoEntity();
        entity.setId(id);
        return entity;
    }

    private Alumno alumno(UUID id) {
        Alumno alumno = new Alumno();
        alumno.setId(id);
        alumno.setUsuario(new Usuario());
        return alumno;
    }
}
