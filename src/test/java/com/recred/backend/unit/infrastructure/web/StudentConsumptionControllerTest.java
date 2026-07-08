package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetLoggedUserConsumptionDetail;
import com.recred.backend.application.GetLoggedUserConsumptionDetailRequest;
import com.recred.backend.application.GetLoggedUserConsumptionDetailResponse;
import com.recred.backend.application.GetStudentRewardStatus;
import com.recred.backend.application.GetStudentRewardStatusRequest;
import com.recred.backend.application.StudentRewardResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.recred.backend.infrastructure.web.StudentConsumptionController;
import com.recred.backend.infrastructure.web.StudentConsumptionExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentConsumptionControllerTest {

    private GetLoggedUserConsumptionDetail getLoggedUserConsumptionDetail;
    private GetStudentRewardStatus getStudentRewardStatus;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        getLoggedUserConsumptionDetail = mock(GetLoggedUserConsumptionDetail.class);
        getStudentRewardStatus = mock(GetStudentRewardStatus.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new StudentConsumptionController(
                        getLoggedUserConsumptionDetail, getStudentRewardStatus))
                .setControllerAdvice(new StudentConsumptionExceptionHandler())
                .build();
    }

    @Test
    void getRewardStatus_DebeRetornarEstadoExitosamente() throws Exception {
        UUID alumnoId = UUID.randomUUID();
        StudentRewardResponse response = new StudentRewardResponse(
                50, "BRONZE", "Mensaje", 50, "SILVER"
        );
        when(getStudentRewardStatus.execute(new GetStudentRewardStatusRequest(alumnoId)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/consumos/alumnos/{alumnoId}/recompensas-saludables", alumnoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoints").value(50))
                .andExpect(jsonPath("$.currentLevel").value("BRONZE"))
                .andExpect(jsonPath("$.pointsToNextLevel").value(50))
                .andExpect(jsonPath("$.nextLevelName").value("SILVER"));

        verify(getStudentRewardStatus).execute(new GetStudentRewardStatusRequest(alumnoId));
    }

    @Test
    void getConsumptionDelegatesAuthenticatedUserAndFilters() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID purchaseId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        GetLoggedUserConsumptionDetailResponse detail =
                new GetLoggedUserConsumptionDetailResponse(
                        studentId,
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 20),
                        new BigDecimal("1500.00"),
                        1,
                        List.of(new GetLoggedUserConsumptionDetailResponse.PurchaseResponse(
                                purchaseId,
                                LocalDateTime.of(2026, 5, 20, 10, 30),
                                "ENTREGADO",
                                "CREDITOS",
                                new BigDecimal("1500.00"),
                                buffetId,
                                "Buffet Primario",
                                List.of(new GetLoggedUserConsumptionDetailResponse.ProductResponse(
                                        productId,
                                        "Sandwich",
                                        "Comida",
                                        1,
                                        new BigDecimal("1500.00"),
                                        new BigDecimal("1500.00"),
                                        List.of()
                                ))
                        ))
                );
        when(getLoggedUserConsumptionDetail.execute(any())).thenReturn(detail);

        mockMvc.perform(get("/api/v1/consumos")
                        .principal(authentication())
                        .param("alumnoId", studentId.toString())
                        .param("desde", "2026-05-01")
                        .param("hasta", "2026-05-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alumnoId").value(studentId.toString()))
                .andExpect(jsonPath("$.desde").value("2026-05-01"))
                .andExpect(jsonPath("$.hasta").value("2026-05-20"))
                .andExpect(jsonPath("$.montoTotal").value(1500.00))
                .andExpect(jsonPath("$.cantidadCompras").value(1))
                .andExpect(jsonPath("$.compras[0].compraId").value(purchaseId.toString()))
                .andExpect(jsonPath("$.compras[0].buffet").value("Buffet Primario"))
                .andExpect(jsonPath("$.compras[0].productos[0].productoId")
                        .value(productId.toString()))
                .andExpect(jsonPath("$.compras[0].productos[0].nombre").value("Sandwich"));

        ArgumentCaptor<GetLoggedUserConsumptionDetailRequest> requestCaptor = ArgumentCaptor
                .forClass(GetLoggedUserConsumptionDetailRequest.class);
        verify(getLoggedUserConsumptionDetail).execute(requestCaptor.capture());
        GetLoggedUserConsumptionDetailRequest request = requestCaptor.getValue();
        assertEquals("cognito-sub", request.cognitoSubject());
        assertEquals(studentId, request.studentId());
        assertEquals(LocalDate.of(2026, 5, 1), request.from());
        assertEquals(LocalDate.of(2026, 5, 20), request.to());
    }

    @Test
    void getConsumptionRejectsMissingAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/consumos")
                        .param("desde", "2026-05-01")
                        .param("hasta", "2026-05-20"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Usuario no autenticado"));

        verifyNoInteractions(getLoggedUserConsumptionDetail);
    }

    private JwtAuthenticationToken authentication() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("cognito-sub")
                .issuedAt(Instant.parse("2026-05-28T10:00:00Z"))
                .expiresAt(Instant.parse("2026-05-28T11:00:00Z"))
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
