package com.recred.backend.infrastructure.openMeteo;

import com.recred.backend.infrastructure.web.WeatherDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OpenMeteoWeatherClientTest {

    private OpenMeteoWeatherClient weatherClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();

        this.mockServer = MockRestServiceServer.bindTo(builder).build();

        this.weatherClient = new OpenMeteoWeatherClient(builder);
    }

    @Test
    void dadoQueOpenMeteoRespondeCorrectamente_cuandoSePideClima_entoncesMapeaYDevuelveWeatherDTO() {
        double latitud = -34.70;
        double longitud = -58.47;

        String jsonRespuestaSimulada = """
                {
                  "current": {
                    "temperature_2m": 15.5,
                    "weather_code": 3
                  }
                }
                """;

        mockServer.expect(requestTo(
                "https://api.open-meteo.com/v1/forecast?latitude=-34.7&longitude=-58.47&current=temperature_2m,weather_code"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonRespuestaSimulada, MediaType.APPLICATION_JSON));
        WeatherDTO resultado = weatherClient.getCurrentWeather(latitud, longitud);

        assertNotNull(resultado);
        assertEquals(15.5, resultado.getTemperature());
        assertEquals("Nublado", resultado.getCondition());
        mockServer.verify();
    }

    @Test
    void dadoQueOpenMeteoRespondeConCodigoDeLluvia_cuandoSePideClima_entoncesTraduceCondicionALluvia() {
        String jsonRespuestaSimulada = """
                {
                  "current": {
                    "temperature_2m": 10.0,
                    "weather_code": 61
                  }
                }
                """;

        mockServer.expect(requestTo(
                "https://api.open-meteo.com/v1/forecast?latitude=10.0&longitude=20.0&current=temperature_2m,weather_code"))
                .andRespond(withSuccess(jsonRespuestaSimulada, MediaType.APPLICATION_JSON));

        WeatherDTO resultado = weatherClient.getCurrentWeather(10.0, 20.0);

        assertEquals(10.0, resultado.getTemperature());
        assertEquals("Lluvia", resultado.getCondition());
    }

    @Test
    void dadoQueOpenMeteoFallaConError500_cuandoSePideClima_entoncesLanzaRuntimeException() {
        mockServer.expect(requestTo(
                "https://api.open-meteo.com/v1/forecast?latitude=0.0&longitude=0.0&current=temperature_2m,weather_code"))
                .andRespond(withServerError());

        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> {
            weatherClient.getCurrentWeather(0.0, 0.0);
        });

        assertEquals("Weather service is currently unavailable", excepcion.getMessage());
    }

    @Test
    void dadoQueOpenMeteoRespondeCuerpoVacio_cuandoSePideClima_entoncesLanzaRuntimeException() {
        mockServer.expect(requestTo(
                "https://api.open-meteo.com/v1/forecast?latitude=0.0&longitude=0.0&current=temperature_2m,weather_code"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> {
            weatherClient.getCurrentWeather(0.0, 0.0);
        });

        assertTrue(excepcion.getCause().getMessage().contains("Received empty response"));
    }
}