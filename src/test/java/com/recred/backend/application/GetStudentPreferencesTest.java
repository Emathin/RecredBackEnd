package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStudentPreferencesTest {

    @Mock
    private SugerenciaIARepository sugerenciaIARepository;
    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private TutorRepository tutorRepository;

    private GetStudentPreferences getStudentPreferences;

    @BeforeEach
    void setUp() {
        getStudentPreferences = new GetStudentPreferences(
                sugerenciaIARepository,
                alumnoRepository,
                tutorRepository);
    }

    @Test
    void parentUserGetsLatestPendingSuggestionForEachChildAndMarksItAccepted() {
        UUID parentUserId = UUID.randomUUID();
        Alumno firstChild = child("Juan", "Perez");
        Alumno secondChild = child("Sofia", "Garcia");
        Tutor tutor = new Tutor();
        tutor.setHijos(java.util.Set.of(firstChild, secondChild));

        SugerenciaIAEntity firstLatest = suggestion(TipoSugerenciaIA.PREFERENCIA_DETECTADA);
        SugerenciaIAEntity firstOlder = suggestion(TipoSugerenciaIA.PREFERENCIA_DETECTADA);
        SugerenciaIAEntity secondLatest = suggestion(TipoSugerenciaIA.PREFERENCIA_DETECTADA);

        when(tutorRepository.findByUsuarioId(parentUserId)).thenReturn(Optional.of(tutor));
        when(sugerenciaIARepository.findPendingParentNotificationsByAlumnoId(
                firstChild.getId(), TipoSugerenciaIA.PREFERENCIA_DETECTADA))
                .thenReturn(List.of(firstLatest, firstOlder));
        when(sugerenciaIARepository.findPendingParentNotificationsByAlumnoId(
                secondChild.getId(), TipoSugerenciaIA.PREFERENCIA_DETECTADA))
                .thenReturn(List.of(secondLatest));

        List<GetStudentPreferences.StudentPreferenceResult> result =
                getStudentPreferences.executeByUserId(
                        parentUserId,
                        false,
                        TipoSugerenciaIA.PREFERENCIA_DETECTADA);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(GetStudentPreferences
                .StudentPreferenceResult::parentView));
        assertTrue(result.stream().map(GetStudentPreferences
                .StudentPreferenceResult::suggestion).toList().contains(firstLatest));
        assertTrue(result.stream().map(GetStudentPreferences
                .StudentPreferenceResult::suggestion).toList().contains(secondLatest));
        assertEquals(Boolean.TRUE, firstLatest.getAceptada());
        assertEquals(Boolean.TRUE, secondLatest.getAceptada());
        assertNotNull(firstLatest.getRespondidaEn());
        assertNotNull(secondLatest.getRespondidaEn());
        verify(sugerenciaIARepository).guardar(firstLatest);
        verify(sugerenciaIARepository).guardar(secondLatest);
        verify(sugerenciaIARepository, never()).guardar(firstOlder);
    }

    @Test
    void studentUserGetsLatestPendingSuggestionAndMarksItSeenByStudent() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Alumno alumno = child("Juan", "Perez");
        alumno.setId(studentId);
        SugerenciaIAEntity latest = suggestion(TipoSugerenciaIA.ALERTA_PRECIO);
        latest.setAceptada(null);
        SugerenciaIAEntity older = suggestion(TipoSugerenciaIA.ALERTA_PRECIO);
        older.setAceptada(null);

        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.empty());
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumno));
        when(sugerenciaIARepository.findPendingStudentNotificationsByAlumnoId(
                studentId,
                TipoSugerenciaIA.ALERTA_PRECIO)).thenReturn(List.of(latest, older));

        List<GetStudentPreferences.StudentPreferenceResult> result =
                getStudentPreferences.executeByUserId(
                        userId,
                        true,
                        TipoSugerenciaIA.ALERTA_PRECIO);

        assertEquals(1, result.size());
        assertEquals(latest, result.get(0).suggestion());
        assertEquals(alumno, result.get(0).child());
        assertTrue(!result.get(0).parentView());
        assertEquals(Boolean.FALSE, latest.getAceptada());
        assertNotNull(latest.getRespondidaEn());
        verify(sugerenciaIARepository).guardar(latest);
        verify(sugerenciaIARepository, never()).guardar(older);
    }

    private Alumno child(String name, String lastName) {
        Usuario user = new Usuario();
        user.setId(UUID.randomUUID());
        user.setNombre(name);
        user.setApellido(lastName);

        Alumno child = new Alumno();
        child.setId(UUID.randomUUID());
        child.setUsuario(user);
        return child;
    }

    private SugerenciaIAEntity suggestion(TipoSugerenciaIA type) {
        SugerenciaIAEntity suggestion = new SugerenciaIAEntity();
        suggestion.setId(UUID.randomUUID());
        suggestion.setTipo(type);
        suggestion.setTitulo("Titulo");
        suggestion.setMensaje("Mensaje");
        suggestion.setAceptada(false);
        return suggestion;
    }
}
