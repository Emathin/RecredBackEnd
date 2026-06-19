package com.recred.backend.web;

import com.recred.backend.application.GetHealthRestrictionsCatalog;
import com.recred.backend.config.CustomAuthenticationEntryPoint;
import com.recred.backend.config.CustomJwtAuthenticationConverter;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.infrastructure.web.ClasificacionSaludController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(
        controllers = ClasificacionSaludController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class}
)
class ClasificacionSaludControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetHealthRestrictionsCatalog getHealthRestrictionsCatalog;

    @MockitoBean
    private CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getCatalogShouldReturnListAndOk() throws Exception {
        ClasificacionSalud item = new ClasificacionSalud();
        item.setId(UUID.randomUUID());
        item.setDescripcion("Vegano");
        when(getHealthRestrictionsCatalog.execute()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/clasificaciones-salud")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].descripcion").value("Vegano"));
    }
}
