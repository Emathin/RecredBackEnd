package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetHistoryByStudent;
import com.recred.backend.application.GetHistoryByStudentRequest;
import com.recred.backend.application.GetHistoryByStudentResponse;
import com.recred.backend.application.GetPendingHistoryByStudent;
import com.recred.backend.application.GetPendingHistoryByStudentRequest;
import com.recred.backend.application.GetPendingHistoryByStudentResponse;
import com.recred.backend.application.GetTutorHistory;
import com.recred.backend.application.GetTutorHistoryRequest;
import com.recred.backend.application.GetTutorHistoryResponse;
import com.recred.backend.config.CustomAuthenticationEntryPoint;
import com.recred.backend.config.CustomJwtAuthenticationConverter;
import com.recred.backend.infrastructure.web.PurchaseHistoryController;
import com.recred.backend.infrastructure.web.PurchaseResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private GetHistoryByStudent getHistoryByStudent;

    @MockitoBean
    private GetTutorHistory getTutorHistory;

    @MockitoBean
    private GetPendingHistoryByStudent getPendingHistoryByStudent;

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

        GetHistoryByStudentResponse response = new GetHistoryByStudentResponse(
                compraId,
                alumnoId,
                new BigDecimal("1500.50"),
                "ENTREGADO",
                "Entregado",
                "EFECTIVO",
                LocalDateTime.now(),
                List.of(),
                "PRESENCIAL",
                null,
                null,
                null,
                null,
                null
        );

        when(getHistoryByStudent.execute(new GetHistoryByStudentRequest(alumnoId))).thenReturn(List.of(response));

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

        GetHistoryByStudentResponse response = new GetHistoryByStudentResponse(
                compraId,
                null,
                new BigDecimal("3000.00"),
                "EN_PREPARACION",
                "En preparacion",
                null,
                LocalDateTime.now(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(getTutorHistory.execute(GetTutorHistoryRequest.byTutorId(tutorId)))
                .thenReturn(new GetTutorHistoryResponse(List.of(response)));

        mockMvc.perform(get("/api/v1/purchases/tutor/{tutorId}", tutorId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(compraId.toString()))
                .andExpect(jsonPath("$[0].totalAmount").value(3000.00))
                .andExpect(jsonPath("$[0].status").value("EN_PREPARACION"));
     }

     @Test
     void cuandoPidePendientesAlumno_entoncesRetorna200YListaDeCompras() throws Exception {
         UUID alumnoId = UUID.randomUUID();
         UUID compraId = UUID.randomUUID();

         GetPendingHistoryByStudentResponse response = new GetPendingHistoryByStudentResponse(
                 compraId,
                 alumnoId,
                 new BigDecimal("1200.00"),
                 "PENDIENTE",
                 "Pendiente",
                 "EFECTIVO",
                 LocalDateTime.now(),
                 List.of(),
                 "PRESENCIAL",
                 null,
                 null,
                 null,
                 null,
                 null
         );

         when(getPendingHistoryByStudent.execute(new GetPendingHistoryByStudentRequest(alumnoId)))
                 .thenReturn(List.of(response));

         mockMvc.perform(get("/api/v1/purchases/alumno/{alumnoId}/pendientes", alumnoId)
                 .contentType(MediaType.APPLICATION_JSON))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$[0].id").value(compraId.toString()))
                 .andExpect(jsonPath("$[0].totalAmount").value(1200.00))
                 .andExpect(jsonPath("$[0].status").value("PENDIENTE"));
     }

    @Test
    void cuandoPideMiHistorialTutor_entoncesUsaElSubjectAutenticado() {
        String cognitoSubject = "cognito-sub";
        UUID compraId = UUID.randomUUID();
        GetHistoryByStudentResponse history = new GetHistoryByStudentResponse(
                compraId,
                null,
                new BigDecimal("2200.00"),
                "ENTREGADO",
                "Entregado",
                null,
                LocalDateTime.now(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null
        );
        PurchaseHistoryController controller = new PurchaseHistoryController(
                getHistoryByStudent,
                getTutorHistory,
                getPendingHistoryByStudent
        );
        when(getTutorHistory.execute(GetTutorHistoryRequest.byCognitoSubject(cognitoSubject)))
                .thenReturn(new GetTutorHistoryResponse(List.of(history)));

        ResponseEntity<List<PurchaseResponseDTO>> response = controller.getMyHistorial(
                new UsernamePasswordAuthenticationToken(cognitoSubject, null)
        );

        assertEquals(1, response.getBody().size());
        assertEquals(compraId, response.getBody().get(0).getId());
    }

    @Test
    void cuandoPideMiHistorialTutorSinAutenticacion_entoncesFalla() {
        PurchaseHistoryController controller = new PurchaseHistoryController(
                getHistoryByStudent,
                getTutorHistory,
                getPendingHistoryByStudent
        );

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.getMyHistorial(null));
    }
}
