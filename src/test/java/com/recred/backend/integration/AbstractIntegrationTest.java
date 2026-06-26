package com.recred.backend.integration;

import java.util.TimeZone;

import com.recred.backend.infrastructure.openMeteo.OpenMeteoWeatherClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Clase base para todos los tests de integración.
 * Centraliza la configuración del contexto de Spring y dependencias compartidas.
 */
@SpringBootTest(properties = {"COGNITO_ISSUER_URI=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_dummy"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static {
      TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));
    }

    @MockitoBean
    protected JwtDecoder jwtDecoder;

    @MockitoBean
    protected OpenMeteoWeatherClient openMeteoWeatherClient;

}
