package com.recred.backend.unit.application;

import com.recred.backend.application.UpdateTutorDashboardConfig;
import com.recred.backend.application.UpdateTutorDashboardConfigRequest;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.TutorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateTutorDashboardConfigTest {

    private static final String COGNITO_SUBJECT = "sub-123";
    private static final String VALID_ARRAY_CONFIG = "[\"card1\", \"card2\", \"card3\", \"card4\", \"card5\"]";
    private static final String VALID_OBJECT_CONFIG = "{\"cards\": [\"card1\", \"card2\", \"card3\"]}";
    private static final String EXCEEDED_CONFIG = "[\"card1\", \"card2\", \"card3\", \"card4\", \"card5\", \"card6\"]";
    private static final String NOT_FOUND_MESSAGE = "Tutor no encontrado para el usuario actual";
    private static final String LIMIT_MESSAGE = "El plan gratuito permite hasta 5 tarjetas en el dashboard";

    @Mock
    private TutorRepository tutorRepository;

    private UpdateTutorDashboardConfig useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateTutorDashboardConfig(tutorRepository);
    }

    @Test
    void shouldSaveConfigWhenFreePlanHasFiveOrFewerCards() {
        Tutor tutor = givenTutorExistsWithPlan(TipoPlan.GRATUITO);

        whenUpdatingConfig(VALID_ARRAY_CONFIG);

        thenConfigWasSaved(tutor, VALID_ARRAY_CONFIG);
    }

    @Test
    void shouldSaveConfigWhenFreePlanObjectHasFiveOrFewerCards() {
        Tutor tutor = givenTutorExistsWithPlan(TipoPlan.GRATUITO);

        whenUpdatingConfig(VALID_OBJECT_CONFIG);

        thenConfigWasSaved(tutor, VALID_OBJECT_CONFIG);
    }

    @Test
    void shouldThrowExceptionWhenFreePlanHasMoreThanFiveCards() {
        givenTutorExistsWithPlan(TipoPlan.GRATUITO);

        Executable execution = () -> whenUpdatingConfig(EXCEEDED_CONFIG);

        thenThrowsIllegalArgumentExceptionWithMessage(execution, LIMIT_MESSAGE);
        thenNoConfigWasSaved();
    }

    @Test
    void shouldSaveConfigWhenAdvancedPlanHasMoreThanFiveCards() {
        Tutor tutor = givenTutorExistsWithPlan(TipoPlan.AVANZADO);

        whenUpdatingConfig(EXCEEDED_CONFIG);

        thenConfigWasSaved(tutor, EXCEEDED_CONFIG);
    }

    @Test
    void shouldThrowExceptionWhenTutorIsNotFound() {
        givenTutorNotFound();

        Executable execution = () -> whenUpdatingConfig(VALID_ARRAY_CONFIG);

        thenThrowsIllegalArgumentExceptionWithMessage(execution, NOT_FOUND_MESSAGE);
        thenNoConfigWasSaved();
    }

    private void whenUpdatingConfig(String configJson) {
        UpdateTutorDashboardConfigRequest request = new UpdateTutorDashboardConfigRequest(
                COGNITO_SUBJECT, configJson);
        useCase.execute(request);
    }

    private Tutor givenTutorExistsWithPlan(TipoPlan plan) {
        Usuario usuario = new Usuario();
        usuario.setPlan(plan);
        Tutor tutor = new Tutor();
        tutor.setUsuario(usuario);
        when(tutorRepository.findByUsuarioIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(tutor));
        return tutor;
    }

    private void givenTutorNotFound() {
        when(tutorRepository.findByUsuarioIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());
    }

    private void thenConfigWasSaved(Tutor tutor, String expectedConfig) {
        assertEquals(expectedConfig, tutor.getDashboardConfig());
        verify(tutorRepository).save(tutor);
    }

    private void thenThrowsIllegalArgumentExceptionWithMessage(Executable execution, String message) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, execution);
        assertEquals(message, exception.getMessage());
    }

    private void thenNoConfigWasSaved() {
        verify(tutorRepository, never()).save(any());
    }
}
