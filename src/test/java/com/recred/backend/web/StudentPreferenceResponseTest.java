package com.recred.backend.web;

import com.recred.backend.application.GetStudentPreferencesResponse;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.repositories.PreferenceAIRepository.PreferenciaDetectada;
import com.recred.backend.infrastructure.web.StudentPreferenceResponseDTO;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static java.util.UUID.randomUUID;
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

        StudentPreferenceResponseDTO response = whenMappingDetectedPreference(preference);

        assertNull(response.sugerenciaId());
        assertNull(response.alumnoId());
        assertEquals(TipoSugerenciaIA.PREFERENCIA_DETECTADA, response.tipo());
        assertEquals("Titulo", response.titulo());
        assertEquals("Mensaje", response.mensaje());
        assertEquals("producto-1", response.productoId());
        assertEquals("Razon", response.razonIA());
    }

    @Test
    void fromApplicationShouldMapPreferenceData() {
        UUID suggestionId = randomUUID();
        UUID studentId = randomUUID();
        UUID studentUserId = randomUUID();

        StudentPreferenceResponseDTO response = whenMappingApplicationPreference(
                preference(suggestionId, studentId, studentUserId, false, "Ana", "Perez"));

        assertEquals(suggestionId, response.sugerenciaId());
        assertEquals(studentId, response.alumnoId());
        assertEquals(studentUserId, response.alumnoUserId());
        assertEquals("Ana Perez", response.alumnoNombre());
        assertEquals(TipoSugerenciaIA.PREFERENCIA_DETECTADA, response.tipo());
        assertEquals("Producto recomendado", response.titulo());
        assertEquals("Compra este producto", response.mensaje());
        assertEquals("producto-1", response.productoId());
        assertEquals("Preferencia detectada", response.razonIA());
    }

    @Test
    void fromApplicationShouldPrefixTitleAndMessageForParentView() {
        UUID studentId = randomUUID();
        UUID studentUserId = randomUUID();

        StudentPreferenceResponseDTO response = whenMappingApplicationPreference(
                preference(randomUUID(), studentId, studentUserId, true, "Mateo", "Fortuna"));

        assertEquals(studentId, response.alumnoId());
        assertEquals(studentUserId, response.alumnoUserId());
        assertEquals("Mateo Fortuna", response.alumnoNombre());
        assertEquals("Mateo Fortuna: Producto recomendado", response.titulo());
        assertEquals("Mateo Fortuna: Compra este producto", response.mensaje());
    }

    @Test
    void fromApplicationShouldUseBaseTextWhenParentViewHasNoStudentName() {
        StudentPreferenceResponseDTO response = whenMappingApplicationPreference(
                preference(randomUUID(), randomUUID(), null, true, null, null));

        assertNull(response.alumnoNombre());
        assertEquals("Producto recomendado", response.titulo());
        assertEquals("Compra este producto", response.mensaje());
    }

    @Test
    void fromApplicationShouldMapNullStudentData() {
        StudentPreferenceResponseDTO response = whenMappingApplicationPreference(
                preference(randomUUID(), null, null, false, null, null));

        assertNull(response.alumnoId());
        assertNull(response.alumnoUserId());
        assertNull(response.alumnoNombre());
        assertEquals("Producto recomendado", response.titulo());
    }

    private StudentPreferenceResponseDTO whenMappingDetectedPreference(
            PreferenciaDetectada preference) {
        return StudentPreferenceResponseDTO.fromDomain(preference);
    }

    private StudentPreferenceResponseDTO whenMappingApplicationPreference(
            GetStudentPreferencesResponse.Preference preference) {
        return StudentPreferenceResponseDTO.fromApplication(preference);
    }

    private GetStudentPreferencesResponse.Preference preference(
            UUID suggestionId,
            UUID studentId,
            UUID studentUserId,
            boolean parentView,
            String firstName,
            String lastName
    ) {
        return new GetStudentPreferencesResponse.Preference(
                suggestionId,
                studentId,
                studentUserId,
                firstName,
                lastName,
                TipoSugerenciaIA.PREFERENCIA_DETECTADA,
                "Producto recomendado",
                "Compra este producto",
                "producto-1",
                "Preferencia detectada",
                parentView
        );
    }
}
