package com.recred.backend.integration;

import java.util.TimeZone;

import com.recred.backend.infrastructure.openMeteo.OpenMeteoWeatherClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
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
