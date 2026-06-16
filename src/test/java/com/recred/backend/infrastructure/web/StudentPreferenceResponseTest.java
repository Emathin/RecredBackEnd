package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.PreferenceAIRepository.PreferenciaDetectada;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StudentPreferenceResponseTest {

    @Test
    void fromDomainShouldMapDetectedPreference() {
        PreferenciaDetectada preference = new PreferenciaDetectada(
                "Titulo",
                "Mensaje",
                "producto-1",
                "Razon"
        );

        StudentPreferenceResponse response = StudentPreferenceResponse.fromDomain(preference);

        assertNull(response.sugerenciaId());
        assertNull(response.alumnoId());
        assertEquals(TipoSugerenciaIA.PREFERENCIA_DETECTADA, response.tipo());
        assertEquals("Titulo", response.titulo());
        assertEquals("Mensaje", response.mensaje());
        assertEquals("producto-1", response.productoId());
        assertEquals("Razon", response.razonIA());
    }

    @Test
    void fromEntityShouldMapSuggestionMetadataAndStudentId() {
        UUID suggestionId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        SugerenciaIAEntity entity = suggestion(suggestionId, alumnoId);

        StudentPreferenceResponse response = StudentPreferenceResponse.fromEntity(entity);

        assertEquals(suggestionId, response.sugerenciaId());
        assertEquals(alumnoId, response.alumnoId());
        assertEquals(TipoSugerenciaIA.PREFERENCIA_DETECTADA, response.tipo());
        assertEquals("Producto recomendado", response.titulo());
        assertEquals("Compra este producto", response.mensaje());
        assertEquals("producto-1", response.productoId());
        assertEquals("Preferencia detectada", response.razonIA());
    }

    @Test
    void fromParentViewShouldPrefixTitleAndMessageWithChildName() {
        SugerenciaIAEntity entity = suggestion(UUID.randomUUID(), UUID.randomUUID());
        Alumno child = child("Mateo", "Fortuna");

        StudentPreferenceResponse response = StudentPreferenceResponse.fromParentView(
                entity,
                child
        );

        assertEquals(child.getId(), response.alumnoId());
        assertEquals(child.getUsuario().getId(), response.alumnoUserId());
        assertEquals("Mateo Fortuna", response.alumnoNombre());
        assertEquals("Mateo Fortuna: Producto recomendado", response.titulo());
        assertEquals("Mateo Fortuna: Compra este producto", response.mensaje());
    }

    @Test
    void fromParentViewShouldUseBaseTextWhenChildHasNoName() {
        SugerenciaIAEntity entity = suggestion(UUID.randomUUID(), UUID.randomUUID());
        Alumno child = child(null, null);

        StudentPreferenceResponse response = StudentPreferenceResponse.fromParentView(
                entity,
                child
        );

        assertNull(response.alumnoNombre());
        assertEquals("Producto recomendado", response.titulo());
        assertEquals("Compra este producto", response.mensaje());
    }

    @Test
    void fromParentViewShouldUseBaseStudentWhenChildIsNull() {
        UUID alumnoId = UUID.randomUUID();
        SugerenciaIAEntity entity = suggestion(UUID.randomUUID(), alumnoId);

        StudentPreferenceResponse response = StudentPreferenceResponse.fromParentView(
                entity,
                null
        );

        assertEquals(alumnoId, response.alumnoId());
        assertNull(response.alumnoUserId());
        assertNull(response.alumnoNombre());
        assertEquals("Producto recomendado", response.titulo());
    }

    @Test
    void fromStudentViewShouldReturnBaseWhenChildIsNull() {
        SugerenciaIAEntity entity = suggestion(UUID.randomUUID(), UUID.randomUUID());

        StudentPreferenceResponse response = StudentPreferenceResponse.fromStudentView(
                entity,
                null
        );

        assertEquals(entity.getId(), response.sugerenciaId());
        assertEquals(entity.getAlumnoEntity().getId(), response.alumnoId());
        assertNull(response.alumnoNombre());
    }

    @Test
    void fromStudentViewShouldMapChildWhenPresent() {
        SugerenciaIAEntity entity = suggestion(UUID.randomUUID(), UUID.randomUUID());
        Alumno child = child("Ana", "Perez");

        StudentPreferenceResponse response = StudentPreferenceResponse.fromStudentView(
                entity,
                child
        );

        assertEquals(child.getId(), response.alumnoId());
        assertEquals(child.getUsuario().getId(), response.alumnoUserId());
        assertEquals("Ana Perez", response.alumnoNombre());
        assertEquals("Producto recomendado", response.titulo());
        assertEquals("Compra este producto", response.mensaje());
    }

    @Test
    void fromStudentViewShouldMapChildWithoutUser() {
        SugerenciaIAEntity entity = suggestion(UUID.randomUUID(), UUID.randomUUID());
        Alumno child = new Alumno();
        child.setId(UUID.randomUUID());

        StudentPreferenceResponse response = StudentPreferenceResponse.fromStudentView(
                entity,
                child
        );

        assertEquals(child.getId(), response.alumnoId());
        assertNull(response.alumnoUserId());
        assertNull(response.alumnoNombre());
    }

    @Test
    void fromEntityShouldHandleNullMetadataAndStudent() {
        SugerenciaIAEntity entity = new SugerenciaIAEntity();
        entity.setId(UUID.randomUUID());
        entity.setTipo(TipoSugerenciaIA.ALERTA_GASTO);
        entity.setTitulo("Titulo");
        entity.setMensaje("Mensaje");

        StudentPreferenceResponse response = StudentPreferenceResponse.fromEntity(entity);

        assertNull(response.alumnoId());
        assertNull(response.productoId());
        assertNull(response.razonIA());
        assertEquals("Titulo", response.titulo());
        assertEquals("Mensaje", response.mensaje());
    }

    private SugerenciaIAEntity suggestion(UUID suggestionId, UUID alumnoId) {
        AlumnoEntity alumno = new AlumnoEntity();
        alumno.setId(alumnoId);

        SugerenciaIAEntity entity = new SugerenciaIAEntity();
        entity.setId(suggestionId);
        entity.setAlumnoEntity(alumno);
        entity.setTipo(TipoSugerenciaIA.PREFERENCIA_DETECTADA);
        entity.setTitulo("Producto recomendado");
        entity.setMensaje("Compra este producto");
        entity.setMetadata(Map.of(
                "productoId", "producto-1",
                "razonIA", "Preferencia detectada"
        ));
        return entity;
    }

    private Alumno child(String firstName, String lastName) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setNombre(firstName);
        usuario.setApellido(lastName);

        Alumno child = new Alumno();
        child.setId(UUID.randomUUID());
        child.setUsuario(usuario);
        return child;
    }
}
