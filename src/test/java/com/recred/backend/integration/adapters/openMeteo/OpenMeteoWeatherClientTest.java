package com.recred.backend.integration.adapters.openMeteo;

import com.recred.backend.infrastructure.openMeteo.OpenMeteoWeatherClient;
import com.recred.backend.infrastructure.web.WeatherDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

  @ParameterizedTest
  @CsvSource({
      "0, Despejado",
      "2, Nublado",
      "61, Lluvia",
      "75, Frío Extremo",
      "96, Tormenta",
      "-1, Indeterminado"
  })
  void dadoUnCodigoDeClima_cuandoSeTraduce_entoncesRetornaLaCondicionCorrecta(Integer codigo,
      String condicionEsperada) {
    String resultado = weatherClient.translateWeatherCode(codigo); // REVISAR

    assertEquals(condicionEsperada, resultado);
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

  @Test
  void dadoQueOpenMeteoRespondeJSONInvalido_cuandoSePideClima_entoncesEntraAlCatchYLanzaExcepcion() {
    mockServer.expect(requestTo(
        "https://api.open-meteo.com/v1/forecast?latitude=0.0&longitude=0.0&current=temperature_2m,weather_code"))
        .andRespond(withSuccess("<html>Error 404 Not Found</html>", MediaType.TEXT_HTML));

    RuntimeException excepcion = assertThrows(RuntimeException.class, () -> {
      weatherClient.getCurrentWeather(0.0, 0.0);
    });

    assertEquals("Weather service is currently unavailable", excepcion.getMessage());
    assertNotNull(excepcion.getCause());
  }
}