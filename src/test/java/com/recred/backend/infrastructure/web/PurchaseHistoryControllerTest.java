package com.recred.backend.infrastructure.web;

import com.recred.backend.application.ObtenerHistorialAlumno;
import com.recred.backend.application.ObtenerHistorialTutor;
import com.recred.backend.config.CustomAuthenticationEntryPoint;
import com.recred.backend.config.CustomJwtAuthenticationConverter;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Purchase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PurchaseHistoryController.class, excludeAutoConfiguration = { SecurityAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class })
class PurchaseHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ObtenerHistorialAlumno obtenerHistorialAlumno;

    @MockitoBean
    private ObtenerHistorialTutor obtenerHistorialTutor;

    @MockitoBean
    private CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void cuandoPideHistorialAlumno_entoncesRetorna200YListaDeCompras() throws Exception {
        UUID alumnoId = UUID.randomUUID();
        UUID compraId = UUID.randomUUID();

        Purchase compra = new Purchase();
        compra.setId(compraId);
        compra.setTotalAmount(new BigDecimal("1500.50"));
        compra.setStatus(EstadoCompra.ENTREGADO);
        compra.setDate(LocalDateTime.now());

        when(obtenerHistorialAlumno.execute(alumnoId)).thenReturn(List.of(compra));

        mockMvc.perform(get("/api/v1/purchases/alumno/{alumnoId}", alumnoId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(compraId.toString()))
                .andExpect(jsonPath("$[0].totalAmount").value(1500.50))
                .andExpect(jsonPath("$[0].status").value("ENTREGADO"));
    }

    @Test
    void cuandoPideHistorialTutor_entoncesRetorna200YListaDeCompras() throws Exception {
        UUID tutorId = UUID.randomUUID();
        UUID compraId = UUID.randomUUID();

        Purchase compra = new Purchase();
        compra.setId(compraId);
        compra.setTotalAmount(new BigDecimal("3000.00"));
        compra.setStatus(EstadoCompra.EN_PREPARACION);
        compra.setDate(LocalDateTime.now());

        when(obtenerHistorialTutor.execute(tutorId)).thenReturn(List.of(compra));

        mockMvc.perform(get("/api/v1/purchases/tutor/{tutorId}", tutorId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(compraId.toString()))
                .andExpect(jsonPath("$[0].totalAmount").value(3000.00))
                .andExpect(jsonPath("$[0].status").value("EN_PREPARACION"));
    }
}
