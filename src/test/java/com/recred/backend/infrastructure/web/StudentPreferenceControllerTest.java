package com.recred.backend.infrastructure.web;

import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.GetStudentPreferences;
import com.recred.backend.application.RespondToAISuggestion;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PreferenceAIRepository.PreferenciaDetectada;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentPreferenceControllerTest {

    @Test
    void returnPreferenceResponseWhenPreferenceIsDetected() {
        DetectStudentPreferences detectStudentPreferences = mock(DetectStudentPreferences.class);
        GetStudentPreferences getStudentPreferences = mock(GetStudentPreferences.class);
        RespondToAISuggestion respondToAISuggestion = mock(RespondToAISuggestion.class);
        AlumnoRepository alumnoRepository = mock(AlumnoRepository.class);
        StudentPreferenceController controller = new StudentPreferenceController(
                detectStudentPreferences, getStudentPreferences, respondToAISuggestion, alumnoRepository);
        
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        PreferenciaDetectada detected = new PreferenciaDetectada(
                "Yogur con Cereales",
                "He notado que te gusta...",
                UUID.randomUUID().toString(),
                "Frecuencia alta"
        );
        when(detectStudentPreferences.execute(studentId, buffetId)).thenReturn(Optional.of(detected));

        ResponseEntity<StudentPreferenceResponse> response = controller.detect(studentId, null, buffetId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(detected.titulo(), response.getBody().titulo());
    }

    @Test
    void returnNoContentWhenNoAIPreferenceIsDetected() {
        DetectStudentPreferences detectStudentPreferences = mock(DetectStudentPreferences.class);
        GetStudentPreferences getStudentPreferences = mock(GetStudentPreferences.class);
        RespondToAISuggestion respondToAISuggestion = mock(RespondToAISuggestion.class);
        AlumnoRepository alumnoRepository = mock(AlumnoRepository.class);
        StudentPreferenceController controller = new StudentPreferenceController(
                detectStudentPreferences, getStudentPreferences, respondToAISuggestion, alumnoRepository);
        
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        when(detectStudentPreferences.execute(studentId, buffetId)).thenReturn(Optional.empty());

        ResponseEntity<StudentPreferenceResponse> response = controller.detect(studentId, null, buffetId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void detectPreferenceUsingUserId() {
        DetectStudentPreferences detectStudentPreferences = mock(DetectStudentPreferences.class);
        GetStudentPreferences getStudentPreferences = mock(GetStudentPreferences.class);
        RespondToAISuggestion respondToAISuggestion = mock(RespondToAISuggestion.class);
        AlumnoRepository alumnoRepository = mock(AlumnoRepository.class);
        StudentPreferenceController controller = new StudentPreferenceController(
                detectStudentPreferences, getStudentPreferences, respondToAISuggestion, alumnoRepository);

        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
        when(detectStudentPreferences.execute(studentId, buffetId)).thenReturn(Optional.empty());

        ResponseEntity<StudentPreferenceResponse> response = controller.detect(null, userId, buffetId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(detectStudentPreferences).execute(studentId, buffetId);
    }

    @Test
    void returnNoContentWhenPreferenceListIsEmpty() {
        DetectStudentPreferences detectStudentPreferences = mock(DetectStudentPreferences.class);
        GetStudentPreferences getStudentPreferences = mock(GetStudentPreferences.class);
        RespondToAISuggestion respondToAISuggestion = mock(RespondToAISuggestion.class);
        AlumnoRepository alumnoRepository = mock(AlumnoRepository.class);
        StudentPreferenceController controller = new StudentPreferenceController(
                detectStudentPreferences, getStudentPreferences, respondToAISuggestion, alumnoRepository);
        
        UUID studentId = UUID.randomUUID();
        when(getStudentPreferences.execute(studentId, false, null)).thenReturn(List.of());

        ResponseEntity<List<StudentPreferenceResponse>> response =
                controller.getPreferences(studentId, null, false, null);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void returnOkWithPreferencesListWhenTheyExist() {
        DetectStudentPreferences detectStudentPreferences = mock(DetectStudentPreferences.class);
        GetStudentPreferences getStudentPreferences = mock(GetStudentPreferences.class);
        RespondToAISuggestion respondToAISuggestion = mock(RespondToAISuggestion.class);
        AlumnoRepository alumnoRepository = mock(AlumnoRepository.class);
        StudentPreferenceController controller = new StudentPreferenceController(
                detectStudentPreferences, getStudentPreferences, respondToAISuggestion, alumnoRepository);
        
        UUID studentId = UUID.randomUUID();
        com.recred.backend.infrastructure.entities.SugerenciaIAEntity entity = 
                new com.recred.backend.infrastructure.entities.SugerenciaIAEntity();
        entity.setTitulo("Test");
        entity.setMensaje("Msg");
        entity.setTipo(com.recred.backend.domain.models.TipoSugerenciaIA.PREFERENCIA_DETECTADA);
        
        when(getStudentPreferences.execute(studentId, false, null)).thenReturn(List.of(entity));

        ResponseEntity<List<StudentPreferenceResponse>> response =
                controller.getPreferences(studentId, null, false, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void respondToAISuggestionSuccessfully() {
        DetectStudentPreferences detectStudentPreferences = mock(DetectStudentPreferences.class);
        GetStudentPreferences getStudentPreferences = mock(GetStudentPreferences.class);
        RespondToAISuggestion respondToAISuggestion = mock(RespondToAISuggestion.class);
        AlumnoRepository alumnoRepository = mock(AlumnoRepository.class);
        StudentPreferenceController controller = new StudentPreferenceController(
                detectStudentPreferences, getStudentPreferences, respondToAISuggestion, alumnoRepository);
        
        UUID sugerenciaId = UUID.randomUUID();
        
        ResponseEntity<Void> response = controller.respond(sugerenciaId, true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(respondToAISuggestion).execute(sugerenciaId, true);
    }
}
