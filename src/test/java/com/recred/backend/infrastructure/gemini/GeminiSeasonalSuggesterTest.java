package com.recred.backend.infrastructure.gemini;

import com.recred.backend.infrastructure.web.RecommendationResponseDTO;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeminiSeasonalSuggesterTest {

    @Mock
    private GeminiTextClient geminiTextClientMock;

    private ObjectMapper realObjectMapper;
    private GeminiSeasonalSuggester service;

    @BeforeEach
    void setUp() {
        realObjectMapper = new ObjectMapper();
        service = new GeminiSeasonalSuggester(geminiTextClientMock, realObjectMapper);
    }

    @Test
    void dadoQueGeminiRespondeConJsonValido_cuandoSeSolicitanSugerencias_entoncesMapeaYDevuelveElDtoCorrectamente() {
        String jsonSimulado = """
                {
                  "sugerencias": [
                    {
                      "categoria": "Bebidas Calientes",
                      "accion": "AUMENTAR",
                      "motivo": "Hace mucho frío."
                    }
                  ],
                  "tip_promocional": "Combo café y alfajor."
                }
                """;

        GeminiTextResponse respuestaSimulada = new GeminiTextResponse(jsonSimulado, "gemini-1.5-flash");
        when(geminiTextClientMock.generate(anyString())).thenReturn(Optional.of(respuestaSimulada));

        RecommendationResponseDTO resultado = service.getSeasonalRecommendations(
                "Julio", 5.0, "Frío");

        assertNotNull(resultado);
        assertEquals("Combo café y alfajor.", resultado.getPromocionalTip());
        assertEquals(1, resultado.getSugerences().size());
        assertEquals("Bebidas Calientes", resultado.getSugerences().get(0).getCategoria());

        verify(geminiTextClientMock, times(1)).generate(anyString());
    }

    @Test
    void dadoQueGeminiRespondeConTextoPlanoEnLugarDeJson_cuandoSeSolicitanSugerencias_entoncesLanzaExcepcionDeParseo() {
        GeminiTextResponse respuestaInvalida = new GeminiTextResponse(
                "Te sugiero vender galletas.", "gemini-1.5-flash");
        when(geminiTextClientMock.generate(anyString())).thenReturn(Optional.of(respuestaInvalida));

        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> {
            service.getSeasonalRecommendations("Julio", 5.0, "Frío");
        });

        assertTrue(excepcion.getMessage().contains("Data parsing error"));
    }

    @Test
    void dadoQueElClienteGeminiNoRetornaRespuesta_cuandoSeSolicitanSugerencias_entoncesLanzaExcepcionDeGeneracionFallida() {
        when(geminiTextClientMock.generate(anyString())).thenReturn(Optional.empty());

        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> {
            service.getSeasonalRecommendations("Julio", 5.0, "Frío");
        });

        assertEquals("Failed to generate seasonal recommendations from AI.", excepcion.getMessage());
    }
}