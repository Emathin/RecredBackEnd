package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GenerateSeasonalRecommendation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = SeasonalRecommendationController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class}
)
class SeasonalRecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GenerateSeasonalRecommendation useCase;

    @Test
    void dadoQueElCasoDeUsoRespondeCorrectamente_cuandoSeLlamaAlEndpoint_entoncesRetorna200ConJson() throws Exception {
        RecommendationResponseDTO respuesta = new RecommendationResponseDTO();
        respuesta.setSugerences(List.of(
                new SugerenciaDTO("Helados y Congelados", "AUMENTAR", "Clima caluroso.")));
        respuesta.setPromocionalTip("Promo 2x1 en helados.");
        when(useCase.execute(anyDouble(), anyDouble())).thenReturn(respuesta);

        mockMvc.perform(get("/api/v1/recomendations/seasonal")
                        .param("latitude", "-34.7")
                        .param("longitude", "-58.47")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tip_promocional").value("Promo 2x1 en helados."))
                .andExpect(jsonPath("$.sugerencias[0].categoria").value("Helados y Congelados"))
                .andExpect(jsonPath("$.sugerencias[0].accion").value("AUMENTAR"));
    }

    @Test
    void dadoQueFaltaElParametroLatitude_cuandoSeLlamaAlEndpoint_entoncesRetornaError() throws Exception {
        mockMvc.perform(get("/api/v1/recomendations/seasonal")
                        .param("longitude", "-58.47")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void dadoQueFaltaElParametroLongitude_cuandoSeLlamaAlEndpoint_entoncesRetornaError() throws Exception {
        mockMvc.perform(get("/api/v1/recomendations/seasonal")
                        .param("latitude", "-34.7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void dadoQueElCasoDeUsoLanzaExcepcion_cuandoSeLlamaAlEndpoint_entoncesRetorna500() throws Exception {
        when(useCase.execute(anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("Weather service is currently unavailable"));

        mockMvc.perform(get("/api/v1/recomendations/seasonal")
                        .param("latitude", "-34.7")
                        .param("longitude", "-58.47")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }
}
