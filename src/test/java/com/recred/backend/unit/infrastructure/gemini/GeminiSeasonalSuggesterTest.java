package com.recred.backend.unit.infrastructure.gemini;

import com.recred.backend.infrastructure.gemini.GeminiSeasonalSuggester;
import com.recred.backend.infrastructure.gemini.GeminiTextClient;
import com.recred.backend.infrastructure.gemini.GeminiTextResponse;
import com.recred.backend.infrastructure.web.RecommendationResponseDTO;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

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
                      "motivo": "Hace mucho frío.",
                      "prioridad": "Alta",
                      "icono": "☕"
                    }
                  ],
                  "tip_promocional": "Combo café y alfajor.",
                  "promocion_sugerida": {
                    "nombre": "Promo Invierno",
                    "descuento": 15.0,
                    "categorias_destino": ["Bebidas Calientes"],
                    "imagen": "cafe.jpg"
                  }
                }
                """;

        GeminiTextResponse respuestaSimulada = new GeminiTextResponse(jsonSimulado, "gemini-1.5-flash");
        when(geminiTextClientMock.generate(anyString())).thenReturn(Optional.of(respuestaSimulada));

        RecommendationResponseDTO resultado = service.getSeasonalRecommendations(
                "Julio", 5.0, "Frío", "Bebidas Calientes");

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
            service.getSeasonalRecommendations("Julio", 5.0, "Frío", "");
        });

        assertTrue(excepcion.getMessage().contains("Data parsing error"));
    }

    @Test
    void dadoQueElClienteGeminiNoRetornaRespuesta_cuandoSeSolicitanSugerencias_entoncesLanzaExcepcionDeGeneracionFallida() {
        when(geminiTextClientMock.generate(anyString())).thenReturn(Optional.empty());

        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> {
            service.getSeasonalRecommendations("Julio", 5.0, "Frío", "");
        });

        assertEquals("Failed to generate seasonal recommendations from AI.", excepcion.getMessage());
    }

    @Test
    void dadoQueGeminiRespondeConJsonVacio_cuandoSeSolicitanSugerencias_entoncesLanzaExcepcionDeParseo() {
        GeminiTextResponse respuestaVacia = new GeminiTextResponse("{}", "gemini-1.5-flash");
        when(geminiTextClientMock.generate(anyString())).thenReturn(Optional.of(respuestaVacia));

        RecommendationResponseDTO resultado = service.getSeasonalRecommendations(
                "Julio", 5.0, "Frío", "");

        assertNotNull(resultado);
        assertNull(resultado.getSugerences());
        assertNull(resultado.getPromocionalTip());
    }

    @Test
    void dadoQueGeminiRespondeConTextoEnBlanco_cuandoSeSolicitanSugerencias_entoncesLanzaExcepcionDeGeneracionFallida() {
        GeminiTextResponse respuestaBlanca = new GeminiTextResponse("   ", "gemini-1.5-flash");
        when(geminiTextClientMock.generate(anyString())).thenReturn(Optional.of(respuestaBlanca));

        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> {
            service.getSeasonalRecommendations("Julio", 5.0, "Frío", "");
        });

        assertEquals("Failed to generate seasonal recommendations from AI.", excepcion.getMessage());
    }

    @ParameterizedTest
    @MethodSource("proveerRespuestasInvalidas")
    void dadoQueLaRespuestaTieneDatosInvalidos_cuandoSeValida_entoncesRetornaFalse(
            GeminiTextResponse respuestaInvalida) {
        boolean resultado = service.isValidResponse(respuestaInvalida);

        assertFalse(resultado);
    }

    private static Stream<GeminiTextResponse> proveerRespuestasInvalidas() {
        return Stream.of(
                null,
                new GeminiTextResponse(null, "modelo"),
                new GeminiTextResponse("", "modelo"),
                new GeminiTextResponse("   \n \t  ", "modelo"));
    }
}

