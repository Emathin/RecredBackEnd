package com.recred.backend.unit.application;

import com.recred.backend.application.UpdateTutorDashboardConfig;
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

        useCase.execute(cognitoSubject, configJson);

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

        useCase.execute(cognitoSubject, configJson);

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

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(cognitoSubject, configJson)
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

        useCase.execute(cognitoSubject, configJson);

        assertEquals(configJson, tutor.getDashboardConfig());
        verify(tutorRepository, times(1)).save(tutor);
    }
}
