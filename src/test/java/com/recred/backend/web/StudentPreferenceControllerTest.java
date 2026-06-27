package com.recred.backend.web;

import com.recred.backend.application.AgregarProductoFavorito;
import com.recred.backend.application.AgregarProductoFavoritoRequest;
import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.GetFavoritosByAlumno;
import com.recred.backend.application.GetFavoritosByAlumnoRequest;
import com.recred.backend.application.GetFavoritosByAlumnoResponse;
import com.recred.backend.application.GetStudentPreferences;
import com.recred.backend.application.GetStudentPreferences.StudentPreferenceResult;
import com.recred.backend.application.RemoverProductoFavorito;
import com.recred.backend.application.RemoverProductoFavoritoRequest;
import com.recred.backend.application.RespondToAISuggestion;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PreferenceAIRepository.PreferenciaDetectada;
import com.recred.backend.infrastructure.web.ProductResponseDTO;
import com.recred.backend.infrastructure.web.StudentPreferenceController;
import com.recred.backend.infrastructure.web.StudentPreferenceResponse;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void returnOkWithParentPreferenceView() {
        UUID userId = UUID.randomUUID();
        Alumno child = new Alumno();
        child.setId(UUID.randomUUID());
        Usuario childUser = new Usuario();
        childUser.setId(UUID.randomUUID());
        childUser.setNombre("Ana");
        childUser.setApellido("Lopez");
        child.setUsuario(childUser);
        com.recred.backend.infrastructure.entities.SugerenciaIAEntity entity =
                new com.recred.backend.infrastructure.entities.SugerenciaIAEntity();
        entity.setTitulo("Probar fruta");
        entity.setMensaje("Puede gustarle");
        entity.setTipo(com.recred.backend.domain.models.TipoSugerenciaIA.PREFERENCIA_DETECTADA);
        when(getStudentPreferences.executeByUserId(userId, false, null))
                .thenReturn(List.of(new StudentPreferenceResult(entity, child, true)));

        ResponseEntity<List<StudentPreferenceResponse>> response =
                controller.getPreferences(userId, false, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Ana Lopez: Probar fruta", response.getBody().get(0).titulo());
        assertEquals(child.getId(), response.getBody().get(0).alumnoId());
        assertEquals(childUser.getId(), response.getBody().get(0).alumnoUserId());
    }

    @Test
    void respondToAISuggestionSuccessfully() {
        UUID sugerenciaId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.respond(sugerenciaId, true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(respondToAISuggestion).execute(sugerenciaId, true);
    }

    @Test
    void agregarFavoritoSuccessfully() {
        UUID studentId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.agregarFavorito(studentId, productoId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(agregarProductoFavorito).execute(new AgregarProductoFavoritoRequest(studentId, productoId));
    }

    @Test
    void removerFavoritoSuccessfully() {
        UUID studentId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.removerFavorito(studentId, productoId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(removerProductoFavorito).execute(new RemoverProductoFavoritoRequest(studentId, productoId));
    }

    @Test
    void obtenerFavoritosSuccessfully() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        GetFavoritosByAlumnoResponse responseDTO = new GetFavoritosByAlumnoResponse(
                productId,
                "Test",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(getFavoritosByAlumno.execute(new GetFavoritosByAlumnoRequest(studentId))).thenReturn(Set.of(responseDTO));

        ResponseEntity<List<ProductResponseDTO>> response = controller.obtenerFavoritos(studentId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(productId, response.getBody().get(0).id());
        verify(getFavoritosByAlumno).execute(new GetFavoritosByAlumnoRequest(studentId));
    }
}
