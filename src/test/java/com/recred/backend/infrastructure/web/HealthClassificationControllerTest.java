package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetHealthRestrictionsCatalog;
import com.recred.backend.domain.models.HealthClassification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;


@WebMvcTest(
        controllers = HealthClassificationController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class}
)
class HealthClassificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetHealthRestrictionsCatalog obtenerCatalogo;

    @Test
    void getCatalog_DeberiaRetornarListaY200Ok() throws Exception {
        HealthClassification item = new HealthClassification();
        item.setId(UUID.randomUUID());
        item.setDescription("Vegano");
        when(obtenerCatalogo.ejecutar()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/clasificaciones-salud")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].descripcion").value("Vegano"));
    }
}