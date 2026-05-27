package com.recred.backend.infrastructure.web;

import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.GetStudentPreferences;
import com.recred.backend.domain.repositories.PreferenceAIRepository.PreferenciaDetectada;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentPreferenceControllerTest {

    @Test
    void shouldReturnPreferenceWhenDetected() {
        DetectStudentPreferences detectStudentPreferences = mock(DetectStudentPreferences.class);
        GetStudentPreferences getStudentPreferences = mock(GetStudentPreferences.class);
        StudentPreferenceController controller = new StudentPreferenceController(
                detectStudentPreferences, getStudentPreferences);
        
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        PreferenciaDetectada detected = new PreferenciaDetectada(
                "Yogur con Cereales",
                "He notado que te gusta...",
                UUID.randomUUID().toString(),
                "Frecuencia alta"
        );
        when(detectStudentPreferences.execute(studentId, buffetId)).thenReturn(Optional.of(detected));

        ResponseEntity<StudentPreferenceResponse> response = controller.detect(studentId, buffetId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(detected.titulo(), response.getBody().titulo());
    }

    @Test
    void shouldReturnNoContentWhenNoPreferenceDetected() {
        DetectStudentPreferences detectStudentPreferences = mock(DetectStudentPreferences.class);
        GetStudentPreferences getStudentPreferences = mock(GetStudentPreferences.class);
        StudentPreferenceController controller = new StudentPreferenceController(
                detectStudentPreferences, getStudentPreferences);
        
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        when(detectStudentPreferences.execute(studentId, buffetId)).thenReturn(Optional.empty());

        ResponseEntity<StudentPreferenceResponse> response = controller.detect(studentId, buffetId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void shouldReturnPreferencesList() {
        DetectStudentPreferences detectStudentPreferences = mock(DetectStudentPreferences.class);
        GetStudentPreferences getStudentPreferences = mock(GetStudentPreferences.class);
        StudentPreferenceController controller = new StudentPreferenceController(
                detectStudentPreferences, getStudentPreferences);
        
        UUID studentId = UUID.randomUUID();
        when(getStudentPreferences.execute(studentId, false)).thenReturn(List.of());

        ResponseEntity<List<StudentPreferenceResponse>> response = controller.getPreferences(studentId, false);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
