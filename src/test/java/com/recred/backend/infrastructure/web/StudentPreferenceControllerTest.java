package com.recred.backend.infrastructure.web;

import com.recred.backend.application.AgregarProductoFavorito;
import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.GetFavoritosByAlumno;
import com.recred.backend.application.GetStudentPreferences;
import com.recred.backend.application.GetStudentPreferences.StudentPreferenceResult;
import com.recred.backend.application.RemoverProductoFavorito;
import com.recred.backend.application.RespondToAISuggestion;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PreferenceAIRepository.PreferenciaDetectada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentPreferenceControllerTest {

    private DetectStudentPreferences detectStudentPreferences;
    private GetStudentPreferences getStudentPreferences;
    private RespondToAISuggestion respondToAISuggestion;
    private AlumnoRepository alumnoRepository;
    private AgregarProductoFavorito agregarProductoFavorito;
    private RemoverProductoFavorito removerProductoFavorito;
    private GetFavoritosByAlumno getFavoritosByAlumno;
    private StudentPreferenceController controller;

    @BeforeEach
    void setUp() {
        detectStudentPreferences = mock(DetectStudentPreferences.class);
        getStudentPreferences = mock(GetStudentPreferences.class);
        respondToAISuggestion = mock(RespondToAISuggestion.class);
        alumnoRepository = mock(AlumnoRepository.class);
        agregarProductoFavorito = mock(AgregarProductoFavorito.class);
        removerProductoFavorito = mock(RemoverProductoFavorito.class);
        getFavoritosByAlumno = mock(GetFavoritosByAlumno.class);

        controller = new StudentPreferenceController(
                detectStudentPreferences,
                getStudentPreferences,
                respondToAISuggestion,
                alumnoRepository,
                agregarProductoFavorito,
                removerProductoFavorito,
                getFavoritosByAlumno
        );
    }

    @Test
    void returnPreferenceResponseWhenPreferenceIsDetected() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        PreferenciaDetectada detected = new PreferenciaDetectada(
                "Yogur con Cereales",
                "He notado que te gusta...",
                UUID.randomUUID().toString(),
                "Frecuencia alta"
        );
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
        when(detectStudentPreferences.execute(studentId, buffetId)).thenReturn(Optional.of(detected));

        ResponseEntity<StudentPreferenceResponse> response = controller.detect(userId, buffetId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(detected.titulo(), response.getBody().titulo());
    }

    @Test
    void returnNoContentWhenNoAIPreferenceIsDetected() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
        when(detectStudentPreferences.execute(studentId, buffetId)).thenReturn(Optional.empty());

        ResponseEntity<StudentPreferenceResponse> response = controller.detect(userId, buffetId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void detectPreferenceUsingUserId() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
        when(detectStudentPreferences.execute(studentId, buffetId)).thenReturn(Optional.empty());

        ResponseEntity<StudentPreferenceResponse> response = controller.detect(userId, buffetId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(detectStudentPreferences).execute(studentId, buffetId);
    }

    @Test
    void returnNoContentWhenPreferenceListIsEmpty() {
        UUID userId = UUID.randomUUID();
        when(getStudentPreferences.executeByUserId(userId, false, null)).thenReturn(List.of());

        ResponseEntity<List<StudentPreferenceResponse>> response =
                controller.getPreferences(userId, false, null);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void returnOkWithPreferencesListWhenTheyExist() {
        UUID userId = UUID.randomUUID();
        com.recred.backend.infrastructure.entities.SugerenciaIAEntity entity =
                new com.recred.backend.infrastructure.entities.SugerenciaIAEntity();
        entity.setTitulo("Test");
        entity.setMensaje("Msg");
        entity.setTipo(com.recred.backend.domain.models.TipoSugerenciaIA.PREFERENCIA_DETECTADA);

        when(getStudentPreferences.executeByUserId(userId, false, null))
                .thenReturn(List.of(new StudentPreferenceResult(entity, null, false)));

        ResponseEntity<List<StudentPreferenceResponse>> response =
                controller.getPreferences(userId, false, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void respondToAISuggestionSuccessfully() {
        UUID sugerenciaId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.respond(sugerenciaId, true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(respondToAISuggestion).execute(sugerenciaId, true);
    }

    @Test
    void agregarFavoritoSuccessfullyWithUserId() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));

        ResponseEntity<Void> response = controller.agregarFavorito(userId, productoId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(agregarProductoFavorito).execute(studentId, productoId);
    }

    @Test
    void removerFavoritoSuccessfullyWithUserId() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));

        ResponseEntity<Void> response = controller.removerFavorito(userId, productoId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(removerProductoFavorito).execute(studentId, productoId);
    }

    @Test
    void obtenerFavoritosSuccessfullyWithUserId() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        producto.setNombre("Test");

        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
        when(getFavoritosByAlumno.execute(studentId)).thenReturn(Set.of(producto));

        ResponseEntity<List<ProductResponseDTO>> response = controller.obtenerFavoritos(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(producto.getId(), response.getBody().get(0).id());
        verify(getFavoritosByAlumno).execute(studentId);
    }
}
