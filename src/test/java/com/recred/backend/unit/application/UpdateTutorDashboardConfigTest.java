package com.recred.backend.unit.application;

import com.recred.backend.application.UpdateTutorDashboardConfig;
import com.recred.backend.application.UpdateTutorDashboardConfigRequest;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.repositories.TutorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateTutorDashboardConfigTest {

    @Mock
    private TutorRepository tutorRepository;

    @InjectMocks
    private UpdateTutorDashboardConfig useCase;

    @Test
    void execute_shouldSaveConfig_whenFreePlanHas5OrFewerCards() {
        String cognitoSubject = "sub-123";
        Usuario usuario = new Usuario();
        usuario.setPlan(TipoPlan.GRATUITO);
        Tutor tutor = new Tutor();
        tutor.setUsuario(usuario);

        String configJson = "[\"card1\", \"card2\", \"card3\", \"card4\", \"card5\"]";

        when(tutorRepository.findByUsuarioIdCognito(cognitoSubject)).thenReturn(Optional.of(tutor));

        UpdateTutorDashboardConfigRequest request = new UpdateTutorDashboardConfigRequest(cognitoSubject, configJson);
        useCase.execute(request);

        assertEquals(configJson, tutor.getDashboardConfig());
        verify(tutorRepository, times(1)).save(tutor);
    }

    @Test
    void execute_shouldSaveConfig_whenFreePlanObjectHas5OrFewerCards() {
        String cognitoSubject = "sub-123";
        Usuario usuario = new Usuario();
        usuario.setPlan(TipoPlan.GRATUITO);
        Tutor tutor = new Tutor();
        tutor.setUsuario(usuario);

        String configJson = "{\"cards\": [\"card1\", \"card2\", \"card3\"]}";

        when(tutorRepository.findByUsuarioIdCognito(cognitoSubject)).thenReturn(Optional.of(tutor));

        UpdateTutorDashboardConfigRequest request = new UpdateTutorDashboardConfigRequest(cognitoSubject, configJson);
        useCase.execute(request);

        assertEquals(configJson, tutor.getDashboardConfig());
        verify(tutorRepository, times(1)).save(tutor);
    }

    @Test
    void execute_shouldThrowException_whenFreePlanHasMoreThan5Cards() {
        String cognitoSubject = "sub-123";
        Usuario usuario = new Usuario();
        usuario.setPlan(TipoPlan.GRATUITO);
        Tutor tutor = new Tutor();
        tutor.setUsuario(usuario);

        String configJson = "[\"card1\", \"card2\", \"card3\", \"card4\", \"card5\", \"card6\"]";

        when(tutorRepository.findByUsuarioIdCognito(cognitoSubject)).thenReturn(Optional.of(tutor));

        UpdateTutorDashboardConfigRequest request = new UpdateTutorDashboardConfigRequest(cognitoSubject, configJson);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(request)
        );

        assertEquals("El plan gratuito permite hasta 5 tarjetas en el dashboard", exception.getMessage());
        verify(tutorRepository, never()).save(any());
    }

    @Test
    void execute_shouldSaveConfig_whenPremiumPlanHasMoreThan5Cards() {
        String cognitoSubject = "sub-123";
        Usuario usuario = new Usuario();
        usuario.setPlan(TipoPlan.PREMIUM);
        Tutor tutor = new Tutor();
        tutor.setUsuario(usuario);

        String configJson = "[\"card1\", \"card2\", \"card3\", \"card4\", \"card5\", \"card6\"]";

        when(tutorRepository.findByUsuarioIdCognito(cognitoSubject)).thenReturn(Optional.of(tutor));

        UpdateTutorDashboardConfigRequest request = new UpdateTutorDashboardConfigRequest(cognitoSubject, configJson);
        useCase.execute(request);

        assertEquals(configJson, tutor.getDashboardConfig());
        verify(tutorRepository, times(1)).save(tutor);
    }

    @Test
    void execute_shouldThrowException_whenTutorNotFound() {
        String cognitoSubject = "non-existent-sub";
        String configJson = "[\"card1\"]";

        when(tutorRepository.findByUsuarioIdCognito(cognitoSubject)).thenReturn(Optional.empty());

        UpdateTutorDashboardConfigRequest request = new UpdateTutorDashboardConfigRequest(cognitoSubject, configJson);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(request)
        );

        assertEquals("Tutor no encontrado para el usuario actual", exception.getMessage());
        verify(tutorRepository, never()).save(any());
    }

    @Test
    void request_shouldThrowException_whenCognitoSubjectIsNull() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                new UpdateTutorDashboardConfigRequest(null, "config").validate()
        );
        assertEquals("El usuario es obligatorio", ex1.getMessage());
    }

    @Test
    void request_shouldThrowException_whenConfigIsNull() {
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                new UpdateTutorDashboardConfigRequest("sub", null).validate()
        );
        assertEquals("La configuración del dashboard es obligatoria", ex2.getMessage());
    }
}
