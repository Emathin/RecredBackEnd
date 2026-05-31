package com.recred.backend.application;

import com.recred.backend.infrastructure.gemini.GeminiSeasonalSuggester;
import com.recred.backend.infrastructure.openMeteo.OpenMeteoWeatherClient;
import com.recred.backend.infrastructure.web.RecommendationResponseDTO;
import com.recred.backend.infrastructure.web.SuggestionDTO;
import com.recred.backend.infrastructure.web.WeatherDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerateSeasonalRecommendationTest {

    @Mock
    private OpenMeteoWeatherClient climaServiceMock;

    @Mock
    private GeminiSeasonalSuggester geminiSuggesterMock;

    @InjectMocks
    private GenerateSeasonalRecommendation useCase;

    @Test
    void dadoQueLosDosServiciosRespondenCorrectamente_cuandoSeEjecuta_entoncesRetornaRecomendaciones() {
        WeatherDTO climaSimulado = new WeatherDTO(25.0, "Despejado");
        when(climaServiceMock.getCurrentWeather(anyDouble(), anyDouble())).thenReturn(climaSimulado);

        RecommendationResponseDTO recomendacionSimulada = new RecommendationResponseDTO();
        recomendacionSimulada.setSugerences(List.of(
                new SuggestionDTO("Helados y Congelados", "AUMENTAR", "Clima caluroso favorece ventas.")));
        recomendacionSimulada.setPromocionalTip("Promo 2x1 en helados.");
        when(geminiSuggesterMock.getSeasonalRecommendations(anyString(), anyDouble(), anyString()))
                .thenReturn(recomendacionSimulada);

        RecommendationResponseDTO resultado = useCase.execute(-34.7, -58.47);

        assertNotNull(resultado);
        assertEquals("Promo 2x1 en helados.", resultado.getPromocionalTip());
        assertEquals(1, resultado.getSugerences().size());
        assertEquals("Helados y Congelados", resultado.getSugerences().get(0).getCategory());
    }

    @Test
    void dadoQueElServicioDeClimaFalla_cuandoSeEjecuta_entoncesLanzaRuntimeException() {
        when(climaServiceMock.getCurrentWeather(anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("Weather service is currently unavailable"));

        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> {
            useCase.execute(-34.7, -58.47);
        });

        assertEquals("Weather service is currently unavailable", excepcion.getMessage());
        verify(geminiSuggesterMock, never()).getSeasonalRecommendations(anyString(), anyDouble(), anyString());
    }

    @Test
    void dadoQueElServicioDeIaFalla_cuandoSeEjecuta_entoncesLanzaRuntimeException() {
        WeatherDTO climaSimulado = new WeatherDTO(5.0, "Frío Extremo");
        when(climaServiceMock.getCurrentWeather(anyDouble(), anyDouble())).thenReturn(climaSimulado);
        when(geminiSuggesterMock.getSeasonalRecommendations(anyString(), anyDouble(), anyString()))
                .thenThrow(new RuntimeException("Failed to generate seasonal recommendations from AI."));

        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> {
            useCase.execute(-34.7, -58.47);
        });

        assertEquals("Failed to generate seasonal recommendations from AI.", excepcion.getMessage());
    }

    @Test
    void dadoQueSeEjecuta_cuandoSeInvoca_entoncesLlamaAlClimaAntesQueLaIa() {
        WeatherDTO climaSimulado = new WeatherDTO(15.0, "Nublado");
        when(climaServiceMock.getCurrentWeather(anyDouble(), anyDouble())).thenReturn(climaSimulado);

        RecommendationResponseDTO recomendacionSimulada = new RecommendationResponseDTO();
        recomendacionSimulada.setSugerences(List.of());
        recomendacionSimulada.setPromocionalTip("Tip genérico.");
        when(geminiSuggesterMock.getSeasonalRecommendations(anyString(), anyDouble(), anyString()))
                .thenReturn(recomendacionSimulada);

        useCase.execute(10.0, 20.0);

        InOrder ordenDeInvocacion = inOrder(climaServiceMock, geminiSuggesterMock);
        ordenDeInvocacion.verify(climaServiceMock).getCurrentWeather(10.0, 20.0);
        ordenDeInvocacion.verify(geminiSuggesterMock)
                .getSeasonalRecommendations(anyString(), eq(15.0), eq("Nublado"));
    }
}
